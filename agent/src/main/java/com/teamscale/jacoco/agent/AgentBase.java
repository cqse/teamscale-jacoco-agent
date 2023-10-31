package com.teamscale.jacoco.agent;

import com.teamscale.client.HttpUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.jacoco.agent.options.FilePatternResolver;
import com.teamscale.jacoco.agent.options.JacocoAgentBuilder;
import com.teamscale.jacoco.agent.options.TeamscaleCredentials;
import com.teamscale.jacoco.agent.options.TeamscalePropertiesUtils;
import com.teamscale.jacoco.agent.util.DebugLogDirectoryPropertyDefiner;
import com.teamscale.jacoco.agent.util.LogDirectoryPropertyDefiner;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.jacoco.agent.util.LoggingUtils.LoggingResources;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jacoco.agent.rt.RT;
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

/**
 * Base class for agent implementations. Handles logger shutdown, store creation and instantiation of the {@link
 * JacocoRuntimeController}.
 * <p>
 * Subclasses must handle dumping onto disk and uploading via the configured uploader.
 */
public abstract class AgentBase {

	/** Environment variable from which to read the config file to use. */
	public static final String CONFIG_ENVIRONMENT_VARIABLE = "TEAMSCALE_JAVA_PROFILER_CONFIG";

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** Controls the JaCoCo runtime. */
	public final JacocoRuntimeController controller;

	/** The agent options. */
	protected AgentOptions options;

	private Server server;

	private static LoggingUtils.LoggingResources loggingResources;

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
				ManagementFactory.getRuntimeMXBean().getName(), getOptionsObjectToLog());
		if (options.getHttpServerPort() != null) {
			try {
				initServer();
			} catch (Exception e) {
				throw new IllegalStateException(
						"Control server not started.", e);
			}
		}
	}

	/**
	 * Lazily generated string representation of the command line arguments to print to the log.
	 */
	private Object getOptionsObjectToLog() {
		return new Object() {
			@Override
			public String toString() {
				if (options.shouldObfuscateSecurityRelatedOutputs()) {
					return options.getObfuscatedOptionsString();
				}
				return options.getOriginalOptionsString();
			}
		};
	}

	/**
	 * Starts the http server, which waits for information about started and finished tests.
	 */
	private void initServer() throws Exception {
		logger.info("Listening for test events on port {}.", options.getHttpServerPort());

		//Jersey Implementation
		ServletContextHandler handler = buildUsingResourceConfig();
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(10);
		threadPool.setDaemon(true);

		// Create a server instance and set the thread pool
		server = new Server(threadPool);

		// Create a server connector, set the port and add it to the server
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(options.getHttpServerPort());
		server.addConnector(connector);
		server.setHandler(handler);
		server.start();
	}

	private ServletContextHandler buildUsingResourceConfig() {
		ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		handler.setContextPath("/");

		ResourceConfig resourceConfig = initResourceConfig();
		handler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");
		return handler;
	}

	/**
	 * Initializes the {@link ResourceConfig} needed for the Jetty + Jersey Server
	 */
	protected abstract ResourceConfig initResourceConfig();

	/**
	 * Called by the actual premain method once the agent is isolated from the rest of the application.
	 */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		String environmentConfigFile = System.getenv(CONFIG_ENVIRONMENT_VARIABLE);
		if (StringUtils.isEmpty(options) && environmentConfigFile == null) {
			// profiler was registered globally and no config was set explicitly by the user, thus ignore this process
			// and don't profile anything
			return;
		}

		DelayedLogger delayedLogger = new DelayedLogger();
		checkForOtherAgents(delayedLogger);

		TeamscaleCredentials credentials = TeamscalePropertiesUtils.parseCredentials();
		AgentOptions agentOptions;
		try {
			agentOptions = AgentOptionsParser.parse(options, environmentConfigFile, credentials, delayedLogger);
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

		initializeLogging(agentOptions, delayedLogger);
		Logger logger = LoggingUtils.getLogger(Agent.class);
		delayedLogger.logTo(logger);

		HttpUtils.setShouldValidateSsl(agentOptions.shouldValidateSsl());

		logger.info("Starting JaCoCo's agent");
		JacocoAgentBuilder agentBuilder = new JacocoAgentBuilder(agentOptions);
		org.jacoco.agent.rt.internal_b6258fc.PreMain.premain(agentBuilder.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentBuilder.createAgent(instrumentation);
		agent.registerShutdownHook();
	}

	private static void checkForOtherAgents(DelayedLogger delayedLogger) {
		List<String> javaAgents = CollectionUtils.filter(ManagementFactory.getRuntimeMXBean().getInputArguments(),
				s -> s.contains("-javaagent"));
		if (javaAgents.size() > 1) {
			delayedLogger.warn("Using multiple java agents could interfere with coverage recording.");
		}
		if (!javaAgents.get(0).contains("teamscale-jacoco-agent.jar")) {
			delayedLogger.warn("For best results consider registering the Teamscale JaCoCo Agent first.");
		}
	}

	/** Initializes logging during {@link #premain(String, Instrumentation)} and also logs the log directory. */
	private static void initializeLogging(AgentOptions agentOptions, DelayedLogger logger) throws IOException {
		if (agentOptions.isDebugLogging()) {
			initializeDebugLogging(agentOptions, logger);
		} else {
			loggingResources = LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());
			logger.info("Logging to " + new LogDirectoryPropertyDefiner().getPropertyValue());
		}
	}

	/**
	 * Initializes debug logging during {@link #premain(String, Instrumentation)} and also logs the log directory if
	 * given.
	 */
	private static void initializeDebugLogging(AgentOptions agentOptions, DelayedLogger logger) {
		loggingResources = LoggingUtils.initializeDebugLogging(agentOptions.getDebugLogDirectory());
		Path logDirectory = Paths.get(new DebugLogDirectoryPropertyDefiner().getPropertyValue());
		if (FileSystemUtils.isValidPath(logDirectory.toString()) && Files.isWritable(logDirectory)) {
			logger.info("Logging to " + logDirectory);
		} else {
			logger.warn("Could not create " + logDirectory + ". Logging to console only.");
		}
	}

	/**
	 * Initializes fallback logging in case of an error during the parsing of the options to {@link #premain(String,
	 * Instrumentation)} (see TS-23151). This tries to extract the logging configuration and use this and falls back to
	 * the default logger.
	 */
	private static LoggingResources initializeFallbackLogging(String premainOptions, DelayedLogger delayedLogger) {
		if (premainOptions == null) {
			return LoggingUtils.initializeDefaultLogging();
		}

		for (String optionPart : premainOptions.split(",")) {
			if (optionPart.startsWith(AgentOptionsParser.LOGGING_CONFIG_OPTION + "=")) {
				return createFallbackLoggerFromConfig(optionPart.split("=", 2)[1], delayedLogger);
			}

			if (optionPart.startsWith(AgentOptionsParser.CONFIG_FILE_OPTION + "=")) {
				String configFileValue = optionPart.split("=", 2)[1];
				Optional<String> loggingConfigLine = Optional.empty();
				try {
					File configFile = new FilePatternResolver(delayedLogger).parsePath(
							AgentOptionsParser.CONFIG_FILE_OPTION, configFileValue).toFile();
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
			return LoggingUtils.initializeLogging(
					new FilePatternResolver(delayedLogger).parsePath(AgentOptionsParser.LOGGING_CONFIG_OPTION,
							configLocation));
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
			try {
				server.stop();
			} catch (Exception e) {
				logger.error("Could not stop server so it is killed now.", e);
			} finally {
				server.destroy();
			}
		}
	}

	/** Called when the shutdown hook is triggered. */
	protected void prepareShutdown() {
		// Template method to be overridden by subclasses.
	}

}
