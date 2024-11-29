package com.teamscale.jacoco.agent

import com.teamscale.client.HttpUtils.setShouldValidateSsl
import com.teamscale.jacoco.agent.configuration.AgentOptionReceiveException
import com.teamscale.jacoco.agent.options.*
import com.teamscale.jacoco.agent.testimpact.TestwiseCoverageAgent
import com.teamscale.jacoco.agent.upload.UploaderException
import com.teamscale.jacoco.agent.util.AgentUtils
import com.teamscale.jacoco.agent.util.DebugLogDirectoryPropertyDefiner
import com.teamscale.jacoco.agent.util.LogDirectoryPropertyDefiner
import com.teamscale.jacoco.agent.util.LoggingUtils
import com.teamscale.jacoco.agent.util.LoggingUtils.LoggingResources
import org.conqat.lib.commons.filesystem.FileSystemUtils
import java.io.IOException
import java.lang.instrument.Instrumentation
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/** Container class for the premain entry point for the agent.  */
object PreMain {
	private var loggingResources: LoggingResources? = null

	/**
	 * System property that we use to prevent this agent from being attached to the same VM twice. This can happen if
	 * the agent is registered via multiple JVM environment variables and/or the command line at the same time.
	 */
	private const val LOCKING_SYSTEM_PROPERTY = "TEAMSCALE_JAVA_PROFILER_ATTACHED"

	/**
	 * Environment variable from which to read the config ID to use. This is an ID for a profiler configuration that is
	 * stored in Teamscale.
	 */
	private const val CONFIG_ID_ENVIRONMENT_VARIABLE = "TEAMSCALE_JAVA_PROFILER_CONFIG_ID"

	/** Environment variable from which to read the config file to use.  */
	private const val CONFIG_FILE_ENVIRONMENT_VARIABLE = "TEAMSCALE_JAVA_PROFILER_CONFIG_FILE"

	private const val JAR_NAME = "teamscale-jacoco-agent.jar"

	/**
	 * Entry point for the agent, called by the JVM.
	 */
	@JvmStatic
	@Throws(Exception::class)
	fun premain(options: String?, instrumentation: Instrumentation) {
		if (System.getProperty(LOCKING_SYSTEM_PROPERTY) != null) return
		System.setProperty(LOCKING_SYSTEM_PROPERTY, "true")

		val environmentConfigId = System.getenv(CONFIG_ID_ENVIRONMENT_VARIABLE)
		val environmentConfigFile = System.getenv(CONFIG_FILE_ENVIRONMENT_VARIABLE)
		if (options.isNullOrBlank() && environmentConfigId == null && environmentConfigFile == null) {
			// profiler was registered globally, and no config was set explicitly by the user, thus ignore this process
			// and don't profile anything
			return
		}

		val agentOptions: AgentOptions
		try {
			agentOptions = getAndApplyAgentOptions(options, environmentConfigId, environmentConfigFile)
		} catch (e: AgentOptionReceiveException) {
			// When Teamscale is not available, we don't want to fail hard to still allow for testing even if no
			// coverage is collected (see TS-33237)
			return
		}

		val logger = LoggingUtils.getLogger(Agent::class.java)

		logger.info("Teamscale Java profiler version " + AgentUtils.VERSION)
		logger.info("Starting JaCoCo's agent")
		val agentBuilder = JacocoAgentOptionsBuilder(agentOptions)
		JaCoCoPreMain.premain(agentBuilder.createJacocoAgentOptions(), instrumentation, logger)

		agentOptions.configurationViaTeamscale?.startHeartbeatThreadAndRegisterShutdownHook()
		val agent = createAgent(agentOptions, instrumentation)
		agent.registerShutdownHook()
	}

	@Throws(AgentOptionParseException::class, IOException::class, AgentOptionReceiveException::class)
	private fun getAndApplyAgentOptions(
		options: String?,
		environmentConfigId: String?,
		environmentConfigFile: String?
	): AgentOptions {
		val delayedLogger = DelayedLogger()
		val javaAgents = ManagementFactory.getRuntimeMXBean()
			.inputArguments.filter { it.contains("-javaagent") }
		// We allow multiple instances of the teamscale-jacoco-agent as
		// we ensure with the #LOCKING_SYSTEM_PROPERTY to only use it once
		if (javaAgents.any { !it.contains(JAR_NAME) }) {
			delayedLogger.warn("Using multiple java agents could interfere with coverage recording.")
		}
		if (!javaAgents.first().contains(JAR_NAME)) {
			delayedLogger.warn("For best results consider registering the Teamscale JaCoCo Agent first.")
		}

		val credentials = TeamscalePropertiesUtils.parseCredentials()
		if (credentials == null) {
			delayedLogger.warn("Did not find a teamscale.properties file!")
		}
		val agentOptions: AgentOptions
		try {
			agentOptions = AgentOptionsParser.parse(
				options, environmentConfigId, environmentConfigFile, credentials, delayedLogger
			)
		} catch (e: AgentOptionParseException) {
			initializeFallbackLogging(options, delayedLogger).use {
				delayedLogger.errorAndStdErr("Failed to parse agent options: ${e.message}", e)
				attemptLogAndThrow(delayedLogger)
				throw e
			}
		} catch (e: AgentOptionReceiveException) {
			initializeFallbackLogging(options, delayedLogger).use {
				delayedLogger.errorAndStdErr(
					"${e.message} The application should start up normally, but NO coverage will be collected!",
					e
				)
				attemptLogAndThrow(delayedLogger)
				throw e
			}
		}

		initializeLogging(agentOptions, delayedLogger)
		val logger = LoggingUtils.getLogger(Agent::class.java)
		delayedLogger.logTo(logger)
		setShouldValidateSsl(agentOptions.shouldValidateSsl())
		return agentOptions
	}

	private fun attemptLogAndThrow(delayedLogger: DelayedLogger) {
		// We perform actual logging output after writing to console to
		// ensure the console is reached even in case of logging issues
		// (see TS-23151). We use the Agent class here (same as below)
		delayedLogger.logTo(LoggingUtils.getLogger(Agent::class.java))
	}

	/** Initializes logging during [.premain] and also logs the log directory.  */
	@Throws(IOException::class)
	private fun initializeLogging(agentOptions: AgentOptions, logger: DelayedLogger) {
		if (agentOptions.isDebugLogging) {
			initializeDebugLogging(agentOptions, logger)
		} else {
			loggingResources = LoggingUtils.initializeLogging(agentOptions.loggingConfig)
			logger.info("Logging to ${LogDirectoryPropertyDefiner().propertyValue}")
		}
	}

	/** Closes the opened logging contexts.  */
	@JvmStatic
	fun closeLoggingResources() {
		loggingResources?.close()
	}

	/**
	 * Returns in instance of the agent that was configured. Either an agent with interval based line-coverage dump or
	 * the HTTP server is used.
	 */
	@Throws(UploaderException::class, IOException::class)
	private fun createAgent(
		agentOptions: AgentOptions,
		instrumentation: Instrumentation
	) = if (agentOptions.useTestwiseCoverageMode()) {
		TestwiseCoverageAgent.create(agentOptions)
	} else {
		Agent(agentOptions, instrumentation)
	}

	/**
	 * Initializes debug logging during [.premain] and also logs the log directory if
	 * given.
	 */
	private fun initializeDebugLogging(agentOptions: AgentOptions, logger: DelayedLogger) {
		loggingResources = LoggingUtils.initializeDebugLogging(agentOptions.debugLogDirectory)
		val logDirectory = Paths.get(DebugLogDirectoryPropertyDefiner().propertyValue)
		if (FileSystemUtils.isValidPath(logDirectory.toString()) && Files.isWritable(logDirectory)) {
			logger.info("Logging to $logDirectory")
		} else {
			logger.warn("Could not create $logDirectory. Logging to console only.")
		}
	}

	/**
	 * Initializes fallback logging in case of an error during the parsing of the options to
	 * [premain] (see TS-23151). This tries to extract the logging configuration and use
	 * this and falls back to the default logger.
	 */
	private fun initializeFallbackLogging(
		premainOptions: String?,
		delayedLogger: DelayedLogger
	): LoggingResources {
		if (premainOptions == null) {
			return LoggingUtils.initializeDefaultLogging()
		}
		premainOptions
			.split(",".toRegex())
			.dropLastWhile { it.isEmpty() }
			.forEach { optionPart ->
				return when {
					optionPart.startsWith("${AgentOptionsParser.LOGGING_CONFIG_OPTION}=") -> {
						createFallbackLoggerFromConfig(
							optionPart.split("=".toRegex(), limit = 2)[1], delayedLogger
						)
					}
					optionPart.startsWith("${AgentOptionsParser.CONFIG_FILE_OPTION}=") -> {
						createConfigFileLogger(optionPart, delayedLogger)
					}
					else -> {
						// Unknown option, log and fall back to default logging
						delayedLogger.warn("Ignoring unknown option: $optionPart")
						LoggingUtils.initializeDefaultLogging()
					}
				}
		}

		return LoggingUtils.initializeDefaultLogging()
	}

	private fun createConfigFileLogger(optionPart: String, delayedLogger: DelayedLogger): LoggingResources {
		val configFileValue = optionPart.split("=".toRegex(), limit = 2)[1]
		try {
			val configFile = FilePatternResolver(delayedLogger).parsePath(
				AgentOptionsParser.CONFIG_FILE_OPTION, configFileValue
			).toFile()
			FileSystemUtils.readLinesUTF8(configFile)
				.find { it.startsWith("${AgentOptionsParser.LOGGING_CONFIG_OPTION}=") }
				?.let {
					return createFallbackLoggerFromConfig(
						it.split("=".toRegex(), limit = 2)[1], delayedLogger
					)
				}
		} catch (e: IOException) {
			delayedLogger.error(
				"Failed to load configuration from $configFileValue: ${e.message}", e
			)
		} catch (e: AgentOptionParseException) {
			delayedLogger.error(
				"Failed to load configuration from $configFileValue: ${e.message}", e
			)
		}
		return LoggingUtils.initializeDefaultLogging()
	}

	/** Creates a fallback logger using the given config file.  */
	private fun createFallbackLoggerFromConfig(
		configLocation: String,
		delayedLogger: DelayedLogger
	) = try {
		LoggingUtils.initializeLogging(
			FilePatternResolver(delayedLogger).parsePath(
				AgentOptionsParser.LOGGING_CONFIG_OPTION, configLocation
			)
		)
	} catch (e: IOException) {
		val message = "Failed to load log configuration from location $configLocation: ${e.message}"
		delayedLogger.error(message, e)
		// output the message to console as well, as this might
		// otherwise not make it to the user
		System.err.println(message)
		LoggingUtils.initializeDefaultLogging()
	} catch (e: AgentOptionParseException) {
		val message = "Failed to load log configuration from location $configLocation: ${e.message}"
		delayedLogger.error(message, e)
		System.err.println(message)
		LoggingUtils.initializeDefaultLogging()
	}
}
