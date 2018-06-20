package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AgentBase {

    /** The logger. */
    protected final Logger logger = LogManager.getLogger(this);

    /** Controls the JaCoCo runtime. */
    protected final JacocoRuntimeController controller;

    /** Stores the XML files. */
    protected final IXmlStore store;

    public AgentBase(AgentOptions options) {
        controller = new JacocoRuntimeController();
        store = options.createStore();

        logger.info("Starting JaCoCo agent with options: {}", options.getOriginalOptionsString());
        logger.info("Storage method: {}", store.describe());
    }

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

    protected abstract void prepareShutdown();
}
