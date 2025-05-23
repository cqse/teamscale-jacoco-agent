package com.teamscale.jacoco.agent

import com.teamscale.client.HttpUtils.setShouldValidateSsl
import com.teamscale.jacoco.agent.PreMain.premain
import com.teamscale.jacoco.agent.configuration.AgentOptionReceiveException
import com.teamscale.jacoco.agent.logging.DebugLogDirectoryPropertyDefiner
import com.teamscale.jacoco.agent.logging.LogDirectoryPropertyDefiner
import com.teamscale.jacoco.agent.logging.LogToTeamscaleAppender
import com.teamscale.jacoco.agent.logging.LoggingUtils
import com.teamscale.jacoco.agent.logging.LoggingUtils.LoggingResources
import com.teamscale.jacoco.agent.options.*
import com.teamscale.jacoco.agent.testimpact.TestwiseCoverageAgent.Companion.create
import com.teamscale.jacoco.agent.upload.UploaderException
import com.teamscale.jacoco.agent.util.AgentUtils
import com.teamscale.report.util.ILogger
import org.conqat.lib.commons.collections.Pair
import org.conqat.lib.commons.filesystem.FileSystemUtils
import java.io.IOException
import java.lang.instrument.Instrumentation
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isWritable

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

	/** Environment variable from which to read the Teamscale access token.  */
	private const val ACCESS_TOKEN_ENVIRONMENT_VARIABLE = "TEAMSCALE_ACCESS_TOKEN"

	/**
	 * Entry point for the agent, called by the JVM.
	 */
	@Throws(Exception::class)
	@JvmStatic
	fun premain(options: String, instrumentation: Instrumentation) {
		if (System.getProperty(LOCKING_SYSTEM_PROPERTY) != null) return
		System.setProperty(LOCKING_SYSTEM_PROPERTY, "true")

		val configId = System.getenv(CONFIG_ID_ENVIRONMENT_VARIABLE)
		val configFile = System.getenv(CONFIG_FILE_ENVIRONMENT_VARIABLE)
		if (options.isEmpty() && configId == null && configFile == null) {
			// profiler was registered globally and no config was set explicitly by the user, thus ignore this process
			// and don't profile anything
			return
		}

		var agentOptions: AgentOptions? = null
		try {
			val parseResult = getAndApplyAgentOptions(options, configId, configFile)
			agentOptions = parseResult.first

			// After parsing everything and configuring logging, we now
			// can throw the caught exceptions.
			parseResult.second?.forEach { exception ->
				throw exception
			}
		} catch (e: AgentOptionParseException) {
			LoggingUtils.getLoggerContext().getLogger(PreMain::class.java).error(e.message, e)

			// Flush logs to Teamscale, if configured.
			closeLoggingResources()

			// Unregister the profiler from Teamscale.
			if (agentOptions != null && agentOptions.configurationViaTeamscale != null) {
				agentOptions.configurationViaTeamscale.unregisterProfiler()
			}

			throw e
		} catch (_: AgentOptionReceiveException) {
			// When Teamscale is not available, we don't want to fail hard to still allow for testing even if no
			// coverage is collected (see TS-33237)
			return
		}

		val logger = LoggingUtils.getLogger(Agent::class.java)

		logger.info("Teamscale Java Profiler version ${AgentUtils.VERSION}")
		logger.info("Starting JaCoCo's agent")
		val agentBuilder = JacocoAgentOptionsBuilder(agentOptions)
		JaCoCoPreMain.premain(agentBuilder.createJacocoAgentOptions(), instrumentation, logger)

		agentOptions.configurationViaTeamscale?.startHeartbeatThreadAndRegisterShutdownHook()
		val agent = createAgent(agentOptions, instrumentation)
		agent.registerShutdownHook()
	}

	@Throws(AgentOptionParseException::class, IOException::class, AgentOptionReceiveException::class)
	private fun getAndApplyAgentOptions(
		options: String,
		environmentConfigId: String?,
		environmentConfigFile: String?
	): Pair<AgentOptions, MutableList<Exception>> {
		val delayedLogger = DelayedLogger()
		val javaAgents = ManagementFactory.getRuntimeMXBean().inputArguments.filter { it.contains("-javaagent") }
		// We allow multiple instances of the teamscale-jacoco-agent as we ensure with the #LOCKING_SYSTEM_PROPERTY to only use it once
		if (!javaAgents.all { it.contains("teamscale-jacoco-agent.jar") }) {
			delayedLogger.warn("Using multiple java agents could interfere with coverage recording.")
		}
		if (!javaAgents[0]!!.contains("teamscale-jacoco-agent.jar")) {
			delayedLogger.warn("For best results consider registering the Teamscale JaCoCo Agent first.")
		}

		val credentials = TeamscalePropertiesUtils.parseCredentials()
		if (credentials == null) {
			// As many users still don't use the installer based setup, this log message will be shown in almost every log.
			// We use a debug log, as this message can be confusing for customers that think a teamscale.properties file is synonymous with a config file.
			delayedLogger.debug(
				"No explicit teamscale.properties file given. Looking for Teamscale credentials in a config file or via a command line argument. This is expected unless the installer based setup was used."
			)
		}

		val environmentAccessToken = System.getenv(ACCESS_TOKEN_ENVIRONMENT_VARIABLE)

		val parseResult: Pair<AgentOptions, MutableList<Exception>>
		val agentOptions: AgentOptions
		try {
			parseResult = AgentOptionsParser.parse(
				options, environmentConfigId, environmentConfigFile, credentials, environmentAccessToken, delayedLogger
			)
			agentOptions = parseResult.getFirst()
		} catch (e: AgentOptionParseException) {
			initializeFallbackLogging(options, delayedLogger).use { ignored ->
				delayedLogger.errorAndStdErr("Failed to parse agent options: " + e.message, e)
				attemptLogAndThrow(delayedLogger)
				throw e
			}
		} catch (e: AgentOptionReceiveException) {
			initializeFallbackLogging(options, delayedLogger).use { ignored ->
				delayedLogger.errorAndStdErr(
					e.message + " The application should start up normally, but NO coverage will be collected! Check the log file for details.",
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

		return parseResult
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
			loggingResources = LoggingUtils.initializeLogging(agentOptions.getLoggingConfig())
			logger.info("Logging to ${LogDirectoryPropertyDefiner().getPropertyValue()}")
		}

		if (agentOptions.teamscaleServerOptions.isConfiguredForServerConnection) {
			if (LogToTeamscaleAppender.addTeamscaleAppenderTo(LoggingUtils.getLoggerContext(), agentOptions)) {
				logger.info("Logs are being forwarded to Teamscale at ${agentOptions.teamscaleServerOptions.url}")
			}
		}
	}

	/** Closes the opened logging contexts.  */
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
		instrumentation: Instrumentation?
	) = if (agentOptions.useTestwiseCoverageMode()) {
		create(agentOptions)
	} else {
		Agent(agentOptions, instrumentation)
	}

	/**
	 * Initializes debug logging during [premain] and also logs the log directory if
	 * given.
	 */
	private fun initializeDebugLogging(agentOptions: AgentOptions, logger: DelayedLogger) {
		loggingResources = LoggingUtils.initializeDebugLogging(agentOptions.getDebugLogDirectory())
		val logDirectory = Paths.get(DebugLogDirectoryPropertyDefiner().getPropertyValue())
		if (FileSystemUtils.isValidPath(logDirectory.toString()) && logDirectory.isWritable()) {
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
		premainOptions: String,
		delayedLogger: DelayedLogger
	): LoggingResources? {
		premainOptions.split(",").dropLastWhile { it.isEmpty() }.forEach { optionPart ->
			if (optionPart.startsWith("${AgentOptionsParser.DEBUG}=")) {
				val value = optionPart.split("=", limit = 2)[1]
				val debugDisabled = value.equals("false", ignoreCase = true)
				val debugEnabled = value.equals("true", ignoreCase = true)
				if (debugDisabled) return@forEach
				var debugLogDirectory: Path? = null
				if (!value.isEmpty() && !debugEnabled) {
					debugLogDirectory = Paths.get(value)
				}
				return LoggingUtils.initializeDebugLogging(debugLogDirectory)
			}
			if (optionPart.startsWith("${AgentOptionsParser.LOGGING_CONFIG_OPTION}=")) {
				return createFallbackLoggerFromConfig(
					optionPart.split("=", limit = 2)[1], delayedLogger
				)
			}

			if (optionPart.startsWith("${AgentOptionsParser.CONFIG_FILE_OPTION}=")) {
				val configFileValue = optionPart.split("=", limit = 2)[1]
				try {
					FilePatternResolver(delayedLogger).parsePath(
						AgentOptionsParser.CONFIG_FILE_OPTION, configFileValue
					).toFile().readLines(Charsets.UTF_8)
						.find { it.startsWith("${AgentOptionsParser.LOGGING_CONFIG_OPTION}=") }
						?.let {
							return createFallbackLoggerFromConfig(
								it.split("=", limit = 2)[1], delayedLogger
							)
						}
				} catch (e: IOException) {
					delayedLogger.error("Failed to load configuration from $configFileValue: ${e.message}", e)
				}
			}
		}

		return LoggingUtils.initializeDefaultLogging()
	}

	/** Creates a fallback logger using the given config file.  */
	private fun createFallbackLoggerFromConfig(
		configLocation: String,
		delayedLogger: ILogger
	): LoggingResources {
		try {
			return LoggingUtils.initializeLogging(
				FilePatternResolver(delayedLogger).parsePath(
					AgentOptionsParser.LOGGING_CONFIG_OPTION,
					configLocation
				)
			)
		} catch (e: IOException) {
			val message = "Failed to load log configuration from location " + configLocation + ": " + e.message
			delayedLogger.error(message, e)
			// output the message to console as well, as this might
			// otherwise not make it to the user
			System.err.println(message)
			return LoggingUtils.initializeDefaultLogging()
		}
	}
}
