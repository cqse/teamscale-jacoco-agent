package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.util.LoggingUtils;
import eu.cqse.teamscale.jacoco.util.LoggingUtils;
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

	/** Stores the XML files. */
	protected final IXmlStore store;

	public AgentBase(AgentOptions options) throws IllegalStateException {
		try {
			controller = new JacocoRuntimeController(RT.getAgent());
		} catch (IllegalStateException e) {
			throw new IllegalStateException(
					"JaCoCo agent not started or there is a conflict with another JaCoCo agent on the classpath.", e);
		}
		store = options.createStore();

		logger.info("Starting JaCoCo agent with options: {}", options.getOriginalOptionsString());
		logger.info("Storage method: {}", store.describe());
	}

	/** Called by the actual premain method once the agent is isolated from the rest of the application. */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		try {
			agentOptions = AgentOptionsParser.parse(options);
		} catch (AgentOptionParseException e) {
			LoggingUtils.initializeDefaultLogging();
			LoggingUtils.getLogger(Agent.class).fatal("Failed to parse agent options: " + e.getMessage(), e);
			System.err.println("Failed to parse agent options: " + e.getMessage());
			LoggingUtils.shutDownLogging();
			throw e;
		}

		LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		LoggingUtils.getLogger(Agent.class).info("Starting JaCoCo's agent");
		org.jacoco.agent.rt.internal_c13123e.PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentOptions.createAgent();
		agent.registerShutdownHook();
	}

	/**
	 * Dumps the current execution data, converts it and writes it to the
	 * {@link #store}. Logs any errors, never throws an exception.
	 */
	protected void dumpReport() {
		logger.debug("Starting dump");

		try {
			dumpReportUnsafe();
		} catch (Throwable t) {
			// we want to catch anything in order to avoid crashing the whole system under test
			logger.error("Dump job failed with an exception", t);
		}
	}

	/**
	 * Performs the actual dump but does not handle e.g. OutOfMemoryErrors.
	 */
	protected abstract void dumpReportUnsafe();

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final
	 * time.
	 */
	public void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			prepareShutdown();
			logger.info("CQSE JaCoCo agent successfully shut down.");
			LoggingUtils.shutDownLogging();
		}));
	}

	/** Called when the shutdown hook is triggered. */
	protected abstract void prepareShutdown();
}
