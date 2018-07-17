package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.agent.rt.RT;

/**
 * Base class for agent implementations. Handles logger shutdown,
 * store creation and instantiation of the {@link JacocoRuntimeController}.
 * <p>
 * Subclasses must handle dumping into the store.
 */
public abstract class AgentBase {

	/** The logger. */
	protected final Logger logger = LogManager.getLogger(this);

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

			// manually shut down the logging system since we prevented automatic shutdown
			LogManager.shutdown();
		}));
	}

	/** Called when the shutdown hook is triggered. */
	protected abstract void prepareShutdown();
}
