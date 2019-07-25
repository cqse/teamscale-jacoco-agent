package com.teamscale.jacoco.agent;

import com.teamscale.jacoco.agent.util.LoggingUtils;
import eu.cqse.teamscale.client.HttpUtils;
import org.jacoco.agent.rt.RT;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;

import static spark.Spark.port;
import static spark.Spark.stop;

/**
 * Base class for agent implementations. Handles logger shutdown, store creation and instantiation of the {@link
 * JacocoRuntimeController}.
 * <p>
 * Subclasses must handle dumping into the store.
 */
public abstract class AgentBase {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** Controls the JaCoCo runtime. */
	protected final JacocoRuntimeController controller;

	/** The agent options. */
	protected AgentOptions options;

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
		port(options.getHttpServerPort());

		initServerEndpoints();
	}

	/** Adds the endpoints that are available in the implemented mode. */
	protected abstract void initServerEndpoints();

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

		HttpUtils.setShouldValidateSsl(agentOptions.validateSsl);

		logger.info("Starting JaCoCo's agent");
		org.jacoco.agent.rt.internal_035b120.PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentOptions.createAgent();
		agent.registerShutdownHook();
	}

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final time.
	 */
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (options.getHttpServerPort() != null) {
				stop();
			}
			prepareShutdown();
			logger.info("CQSE JaCoCo agent successfully shut down.");
			loggingResources.close();
		}));
	}

	/** Called when the shutdown hook is triggered. */
	protected void prepareShutdown() {
	}
}
