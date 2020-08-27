package com.teamscale.jacoco.agent;

import com.teamscale.client.HttpUtils;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.jacoco.agent.options.JacocoAgentBuilder;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.jacoco.agent.rt.RT;
import org.slf4j.Logger;
import spark.Service;

import java.lang.instrument.Instrumentation;

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

		logger.info("Starting JaCoCo agent with options: {}", options.getOriginalOptionsString());
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
		// this is needed during our tests which will try to access the API directly after creating an agent
		spark.awaitInitialization();
	}

	/** Adds the endpoints that are available in the implemented mode. */
	protected abstract void initServerEndpoints(Service spark);

	/** Called by the actual premain method once the agent is isolated from the rest of the application. */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		DelayedLogger delayedLogger = new DelayedLogger();
		try {
			agentOptions = AgentOptionsParser.parse(options, delayedLogger);
		} catch (AgentOptionParseException e) {
			try (LoggingUtils.LoggingResources ignored = LoggingUtils.initializeDefaultLogging()) {
				Logger logger = LoggingUtils.getLogger(PreMain.class);
				delayedLogger.logTo(logger);
				logger.error("Failed to parse agent options: " + e.getMessage(), e);
				System.err.println("Failed to parse agent options: " + e.getMessage());
				throw e;
			}
		}

		loggingResources = LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		Logger logger = LoggingUtils.getLogger(Agent.class);
		delayedLogger.logTo(logger);

		HttpUtils.setShouldValidateSsl(agentOptions.shouldValidateSsl());

		logger.info("Starting JaCoCo's agent");
		JacocoAgentBuilder agentBuilder = new JacocoAgentBuilder(agentOptions);
		org.jacoco.agent.rt.internal_43f5073.PreMain
				.premain(agentBuilder.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentBuilder.createAgent(instrumentation);
		agent.registerShutdownHook();
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
}
