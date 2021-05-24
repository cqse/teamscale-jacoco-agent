package com.teamscale.jacoco.agent;

import com.teamscale.client.HttpUtils;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.jacoco.agent.options.FilePatternResolver;
import com.teamscale.jacoco.agent.options.JacocoAgentBuilder;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.jacoco.agent.util.LoggingUtils.LoggingResources;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.agent.rt.RT;
import org.slf4j.Logger;
import spark.Request;
import spark.Response;
import spark.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.Optional;

/**
 * Base class for agent implementations. Handles logger shutdown, store creation and instantiation of the {@link
 * JacocoRuntimeController}.
 * <p>
 * Subclasses must handle dumping onto disk and uploading via the configured uploader.
 */
public abstract class AgentBase {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** Controls the JaCoCo runtime. */
	protected final JacocoRuntimeController controller;

	/** The agent options. */
	protected AgentOptions options;

	private static LoggingUtils.LoggingResources loggingResources;

	private final Service spark = Service.ignite();

	/** Constructor. */
	public AgentBase(AgentOptions options) throws IllegalStateException {
		this.options = options;
		try {
			controller = new JacocoRuntimeController(RT.getAgent());
		} catch (IllegalStateException e) {
			throw new IllegalStateException(
					"JaCoCo agent not started or there is a conflict with another JaCoCo agent on the classpath.", e);
		}

		logger.info("Starting JaCoCo agent for process {} with options: {}",
				ManagementFactory.getRuntimeMXBean().getName(), options.getOriginalOptionsString());
		if (options.getHttpServerPort() != null) {
			initServer();
		}
	}

	/**
	 * Starts the http server, which waits for information about started and finished tests.
	 */
	private void initServer() {
		logger.info("Listening for test events on port {}.", options.getHttpServerPort());
		spark.port(options.getHttpServerPort());

		initServerEndpoints(spark);
		// this is needed during our tests which will try to access the API
		// directly after creating an agent
		spark.awaitInitialization();
	}

	/** Adds the endpoints that are available in the implemented mode. */
	protected void initServerEndpoints(Service spark) {
		spark.get("/partition", (request, response) ->
				Optional.ofNullable(options.getTeamscaleServerOptions().partition).orElse(""));
		spark.get("/message", (request, response) ->
				Optional.ofNullable(options.getTeamscaleServerOptions().getMessage()).orElse(""));
		spark.put("/partition", this::handleSetPartition);
		spark.put("/message", this::handleSetMessage);
	}

	/**
	 * Called by the actual premain method once the agent is isolated from the rest of the application.
	 */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		DelayedLogger delayedLogger = new DelayedLogger();
		try {
			agentOptions = AgentOptionsParser.parse(options, delayedLogger);
		} catch (AgentOptionParseException e) {
			try (LoggingUtils.LoggingResources ignored = initializeFallbackLogging(options, delayedLogger)) {
				delayedLogger.error("Failed to parse agent options: " + e.getMessage(), e);
				System.err.println("Failed to parse agent options: " + e.getMessage());

				// we perform actual logging output after writing to console to
				// ensure the console is reached even in case of logging issues
				// (see TS-23151). We use the Agent class here (same as below)
				Logger logger = LoggingUtils.getLogger(Agent.class);
				delayedLogger.logTo(logger);
				throw e;
			}
		}

		loggingResources = LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		Logger logger = LoggingUtils.getLogger(Agent.class);
		delayedLogger.logTo(logger);

		HttpUtils.setShouldValidateSsl(agentOptions.shouldValidateSsl());

		logger.info("Starting JaCoCo's agent");
		JacocoAgentBuilder agentBuilder = new JacocoAgentBuilder(agentOptions);
		org.jacoco.agent.rt.internal_3570298.PreMain.premain(agentBuilder.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentBuilder.createAgent(instrumentation);
		agent.registerShutdownHook();
	}

	/**
	 * Initializes fallback logging in case of an error during the parsing of the options to {@link #premain(String,
	 * Instrumentation)} (see TS-23151). This tries to extract the logging configuration and use this and falls back to
	 * the default logger.
	 */
	private static LoggingResources initializeFallbackLogging(String premainOptions, DelayedLogger delayedLogger) {
		for (String optionPart : premainOptions.split(",")) {
			if (optionPart.startsWith(AgentOptionsParser.LOGGING_CONFIG_OPTION + "=")) {
				return createFallbackLoggerFromConfig(optionPart.split("=", 2)[1], delayedLogger);
			}

			if (optionPart.startsWith(AgentOptionsParser.CONFIG_FILE_OPTION + "=")) {
				String configFileValue = optionPart.split("=", 2)[1];
				Optional<String> loggingConfigLine = Optional.empty();
				try {
					File configFile = new FilePatternResolver(delayedLogger)
							.parsePath(AgentOptionsParser.CONFIG_FILE_OPTION, configFileValue).toFile();
					loggingConfigLine = FileSystemUtils.readLinesUTF8(configFile).stream()
							.filter(line -> line.startsWith(AgentOptionsParser.LOGGING_CONFIG_OPTION + "="))
							.findFirst();
				} catch (IOException | AgentOptionParseException e) {
					delayedLogger.error("Failed to load configuration from " + configFileValue + ": " + e.getMessage(),
							e);
				}
				if (loggingConfigLine.isPresent()) {
					return createFallbackLoggerFromConfig(loggingConfigLine.get().split("=", 2)[1], delayedLogger);
				}
			}
		}

		return LoggingUtils.initializeDefaultLogging();
	}

	/** Creates a fallback logger using the given config file. */
	private static LoggingResources createFallbackLoggerFromConfig(String configLocation, DelayedLogger delayedLogger) {
		try {
			return LoggingUtils.initializeLogging(new FilePatternResolver(delayedLogger)
					.parsePath(AgentOptionsParser.LOGGING_CONFIG_OPTION, configLocation));
		} catch (IOException | AgentOptionParseException e) {
			String message = "Failed to load log configuration from location " + configLocation + ": " + e.getMessage();
			delayedLogger.error(message, e);
			// output the message to console as well, as this might
			// otherwise not make it to the user
			System.err.println(message);
			return LoggingUtils.initializeDefaultLogging();
		}
	}

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final time.
	 */
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			stopServer();
			prepareShutdown();
			logger.info("CQSE JaCoCo agent successfully shut down.");
			loggingResources.close();
		}));
	}

	/** Stop the http server if it's running */
	void stopServer() {
		if (options.getHttpServerPort() != null) {
			spark.stop();
		}
	}

	/** Called when the shutdown hook is triggered. */
	protected void prepareShutdown() {
		// Template method to be overridden by subclasses.
	}

	/** Handles setting the partition name. */
	private String handleSetPartition(Request request, Response response) {
		String partition = StringUtils.removeDoubleQuotes(request.body());
		if (partition == null || partition.isEmpty()) {
			String errorMessage = "The new partition name is missing in the request body! Please add it as plain text.";
			logger.error(errorMessage);

			response.status(HttpServletResponse.SC_BAD_REQUEST);
			return errorMessage;
		}

		logger.debug("Changing partition name to " + partition);
		controller.setSessionId(partition);
		options.getTeamscaleServerOptions().partition = partition;

		response.status(HttpServletResponse.SC_NO_CONTENT);
		return "";
	}

	/** Handles setting the partition name. */
	private String handleSetMessage(Request request, Response response) {
		String message =  StringUtils.removeDoubleQuotes(request.body());
		if (message == null || message.isEmpty()) {
			String errorMessage = "The new message is missing in the request body! Please add it as plain text.";
			logger.error(errorMessage);

			response.status(HttpServletResponse.SC_BAD_REQUEST);
			return errorMessage;
		}

		logger.debug("Changing message to " + message);
		options.getTeamscaleServerOptions().setMessage(message);

		response.status(HttpServletResponse.SC_NO_CONTENT);
		return "";
	}
}
