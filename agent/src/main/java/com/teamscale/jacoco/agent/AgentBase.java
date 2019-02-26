package com.teamscale.jacoco.agent;

import com.teamscale.jacoco.util.LoggingUtils;
import eu.cqse.teamscale.client.HttpUtils;
import org.jacoco.agent.rt.RT;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;

/**
 * Base class for agent implementations. Handles logger shutdown,
 * store creation and instantiation of the {@link JacocoRuntimeController}.
 * <p>
 * Subclasses must handle dumping into the store.
 */
public abstract class AgentBase {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** Controls the JaCoCo runtime. */
	protected final JacocoRuntimeController controller;

	private static LoggingUtils.LoggingResources loggingResources;

	/** Constructor. */
	public AgentBase(AgentOptions options) throws IllegalStateException {
		try {
			controller = new JacocoRuntimeController(RT.getAgent());
		} catch (IllegalStateException e) {
			throw new IllegalStateException(
					"JaCoCo agent not started or there is a conflict with another JaCoCo agent on the classpath.", e);
		}

		logger.info("Starting JaCoCo agent with options: {}", options.getOriginalOptionsString());
	}

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
		org.jacoco.agent.rt.internal_1f1cc91.PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentOptions.createAgent();
		agent.registerShutdownHook();
	}

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final
	 * time.
	 */
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			prepareShutdown();
			logger.info("CQSE JaCoCo agent successfully shut down.");
			loggingResources.close();
		}));
	}

	/** Called when the shutdown hook is triggered. */
	protected abstract void prepareShutdown();
}
