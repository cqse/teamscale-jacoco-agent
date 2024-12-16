package com.teamscale.jacoco.agent;

import com.teamscale.client.HttpUtils;
import com.teamscale.jacoco.agent.configuration.AgentOptionReceiveException;
import com.teamscale.jacoco.agent.logging.LogToTeamscaleAppender;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.jacoco.agent.options.FilePatternResolver;
import com.teamscale.jacoco.agent.options.JacocoAgentOptionsBuilder;
import com.teamscale.jacoco.agent.options.TeamscaleCredentials;
import com.teamscale.jacoco.agent.options.TeamscalePropertiesUtils;
import com.teamscale.jacoco.agent.testimpact.TestwiseCoverageAgent;
import com.teamscale.jacoco.agent.upload.UploaderException;
import com.teamscale.jacoco.agent.util.AgentUtils;
import com.teamscale.jacoco.agent.logging.DebugLogDirectoryPropertyDefiner;
import com.teamscale.jacoco.agent.logging.LogDirectoryPropertyDefiner;
import com.teamscale.jacoco.agent.logging.LoggingUtils;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.teamscale.jacoco.agent.logging.LoggingUtils.getLoggerContext;

/** Container class for the premain entry point for the agent. */
public class PreMain {

	private static final Logger LOGGER = LoggingUtils.getLogger(PreMain.class);

	private static LoggingUtils.LoggingResources loggingResources = null;

	/**
	 * System property that we use to prevent this agent from being attached to the same VM twice. This can happen if
	 * the agent is registered via multiple JVM environment variables and/or the command line at the same time.
	 */
	private static final String LOCKING_SYSTEM_PROPERTY = "TEAMSCALE_JAVA_PROFILER_ATTACHED";

	/** Environment variable from which to read the config ID to use.
	 * This is an ID for a profiler configuration that is stored in Teamscale. */
	private static final String CONFIG_ID_ENVIRONMENT_VARIABLE = "TEAMSCALE_JAVA_PROFILER_CONFIG_ID";

	/** Environment variable from which to read the config file to use. */
	private static final String CONFIG_FILE_ENVIRONMENT_VARIABLE = "TEAMSCALE_JAVA_PROFILER_CONFIG_FILE";

	/**
	 * Entry point for the agent, called by the JVM.
	 */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		if (System.getProperty(LOCKING_SYSTEM_PROPERTY) != null) {
			return;
		}
		System.setProperty(LOCKING_SYSTEM_PROPERTY, "true");

		String environmentConfigId = System.getenv(CONFIG_ID_ENVIRONMENT_VARIABLE);
		String environmentConfigFile = System.getenv(CONFIG_FILE_ENVIRONMENT_VARIABLE);
		if (StringUtils.isEmpty(options) && environmentConfigId == null && environmentConfigFile == null) {
			// profiler was registered globally and no config was set explicitly by the user, thus ignore this process
			// and don't profile anything
			return;
		}

		AgentOptions agentOptions;
		try {
			agentOptions = getAndApplyAgentOptions(options, environmentConfigId, environmentConfigFile);
		} catch (AgentOptionReceiveException e) {
			// When Teamscale is not available, we don't want to fail hard to still allow for testing even if no
			// coverage is collected (see TS-33237)
			return;
		}

		Logger logger = LoggingUtils.getLogger(Agent.class);

		logger.info("Teamscale Java profiler version {}", AgentUtils.VERSION);
		logger.info("Starting JaCoCo's agent");
		JacocoAgentOptionsBuilder agentBuilder = new JacocoAgentOptionsBuilder(agentOptions);
		JaCoCoPreMain.premain(agentBuilder.createJacocoAgentOptions(), instrumentation, logger);

		if (agentOptions.configurationViaTeamscale != null) {
			agentOptions.configurationViaTeamscale.startHeartbeatThreadAndRegisterShutdownHook();
		}
		AgentBase agent = createAgent(agentOptions, instrumentation);
		agent.registerShutdownHook();
	}

	@NotNull
	private static AgentOptions getAndApplyAgentOptions(String options, String environmentConfigId,
														String environmentConfigFile) throws AgentOptionParseException, IOException, AgentOptionReceiveException {

		List<String> javaAgents = CollectionUtils.filter(ManagementFactory.getRuntimeMXBean().getInputArguments(),
				s -> s.contains("-javaagent"));
		if (javaAgents.size() > 1) {
			LOGGER.warn("Using multiple java agents could interfere with coverage recording.");
		}
		if (!javaAgents.get(0).contains("teamscale-jacoco-agent.jar")) {
			LOGGER.warn("For best results consider registering the Teamscale JaCoCo Agent first.");
		}

		TeamscaleCredentials credentials = TeamscalePropertiesUtils.parseCredentials();
		if (credentials == null) {
			LOGGER.warn("Did not find a teamscale.properties file!");
		}
		AgentOptions agentOptions;
		try {
			agentOptions = AgentOptionsParser.parse(options, environmentConfigId, environmentConfigFile, credentials);
		} catch (AgentOptionParseException e) {
			try (LoggingUtils.LoggingResources ignored = initializeFallbackLogging(options)) {
				String message = "Failed to parse agent options: " + e.getMessage();
				logAndPrintError(e, message);
				throw e;
			}
		} catch (AgentOptionReceiveException e) {
			try (LoggingUtils.LoggingResources ignored = initializeFallbackLogging(options)) {
				String message = e.getMessage() + " The application should start up normally, but NO coverage will be collected!";
				logAndPrintError(e, message);
				throw e;
			}
		}

		initializeLogging(agentOptions);
		HttpUtils.setShouldValidateSsl(agentOptions.shouldValidateSsl());
		return agentOptions;
	}

	/** Initializes logging during {@link #premain(String, Instrumentation)} and also logs the log directory. */
	private static void initializeLogging(AgentOptions agentOptions) throws IOException {
		if (agentOptions.isDebugLogging()) {
			initializeDebugLogging(agentOptions);
		} else {
			loggingResources = LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());
			LOGGER.info("Logging to " + new LogDirectoryPropertyDefiner().getPropertyValue());
		}

		if (agentOptions.getTeamscaleServerOptions().isConfiguredForServerConnection()) {
			LogToTeamscaleAppender.addTeamscaleAppenderTo(getLoggerContext(), agentOptions);
		}
	}

	/** Closes the opened logging contexts. */
	static void closeLoggingResources() {
		loggingResources.close();
	}

	/**
	 * Returns in instance of the agent that was configured. Either an agent with interval based line-coverage dump or
	 * the HTTP server is used.
	 */
	private static AgentBase createAgent(AgentOptions agentOptions,
										 Instrumentation instrumentation) throws UploaderException, IOException {
		if (agentOptions.useTestwiseCoverageMode()) {
			return TestwiseCoverageAgent.create(agentOptions);
		} else {
			return new Agent(agentOptions, instrumentation);
		}
	}

	/**
	 * Initializes debug logging during {@link #premain(String, Instrumentation)} and also logs the log directory if
	 * given.
	 */
	private static void initializeDebugLogging(AgentOptions agentOptions) {
		loggingResources = LoggingUtils.initializeDebugLogging(agentOptions.getDebugLogDirectory());
		Path logDirectory = Paths.get(new DebugLogDirectoryPropertyDefiner().getPropertyValue());
		if (FileSystemUtils.isValidPath(logDirectory.toString()) && Files.isWritable(logDirectory)) {
			LOGGER.info("Logging to " + logDirectory);
		} else {
			LOGGER.warn("Could not create " + logDirectory + ". Logging to console only.");
		}
	}

	/**
	 * Initializes fallback logging in case of an error during the parsing of the options to
	 * {@link #premain(String, Instrumentation)} (see TS-23151). This tries to extract the logging configuration and use
	 * this and falls back to the default logger.
	 */
	private static LoggingUtils.LoggingResources initializeFallbackLogging(String premainOptions) {
		if (premainOptions == null) {
			return LoggingUtils.initializeDefaultLogging();
		}
		for (String optionPart : premainOptions.split(",")) {
			if (optionPart.startsWith(AgentOptionsParser.LOGGING_CONFIG_OPTION + "=")) {
				return createFallbackLoggerFromConfig(optionPart.split("=", 2)[1]);
			}

			if (optionPart.startsWith(AgentOptionsParser.CONFIG_FILE_OPTION + "=")) {
				String configFileValue = optionPart.split("=", 2)[1];
				Optional<String> loggingConfigLine = Optional.empty();
				try {
					File configFile = new FilePatternResolver().parsePath(
							AgentOptionsParser.CONFIG_FILE_OPTION, configFileValue).toFile();
					loggingConfigLine = FileSystemUtils.readLinesUTF8(configFile).stream()
							.filter(line -> line.startsWith(AgentOptionsParser.LOGGING_CONFIG_OPTION + "="))
							.findFirst();
				} catch (IOException | AgentOptionParseException e) {
					String message = "Failed to load configuration from " + configFileValue + ": " + e.getMessage();
					logAndPrintError(e, message);
				}
				if (loggingConfigLine.isPresent()) {
					return createFallbackLoggerFromConfig(loggingConfigLine.get().split("=", 2)[1]);
				}
			}
		}

		return LoggingUtils.initializeDefaultLogging();
	}

	/** Creates a fallback logger using the given config file. */
	private static LoggingUtils.LoggingResources createFallbackLoggerFromConfig(String configLocation) {
		try {
			return LoggingUtils.initializeLogging(
					new FilePatternResolver().parsePath(AgentOptionsParser.LOGGING_CONFIG_OPTION,
							configLocation));
		} catch (IOException | AgentOptionParseException e) {
			String message = "Failed to load log configuration from location " + configLocation + ": " + e.getMessage();
			logAndPrintError(e, message);
			return LoggingUtils.initializeDefaultLogging();
		}
	}

	/**
	 * Log the error and also print it to System Error, as the error might prevent the initialization of a real logger.
	 */
	private static void logAndPrintError(Exception e, String message) {
		LOGGER.error(message,
				e);
		System.err.println(message);
	}
}
