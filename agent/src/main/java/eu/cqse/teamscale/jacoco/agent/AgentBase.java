package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.dump.Dump;
import eu.cqse.teamscale.jacoco.agent.dump.JacocoRuntimeController;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

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

    /**
     * Dumps the current execution data, converts it and writes it to the
     * {@link #store}. Logs any errors, never throws an exception.
     */
    protected void dump() {
        logger.debug("Starting dump");

        try {
            dumpUnsafe();
        } catch (Throwable t) {
            // we want to catch anything in order to avoid killing the regular job
            logger.error("Dump job failed with an exception. Retrying later", t);
        }
    }

    /**
     * Performs the actual dump but does not handle e.g. OutOfMemoryErrors.
     */
    private void dumpUnsafe() {
        Dump dump;
        try {
            dump = controller.dumpAndReset();
        } catch (JacocoRuntimeController.DumpException e) {
            logger.error("Dumping failed, retrying later", e);
            return;
        }

        String xml;
        try {
            xml = generateReport(dump);
        } catch (IOException e) {
            logger.error("Converting binary dump to XML failed", e);
            return;
        }

        store.store(xml);
    }

    protected abstract String generateReport(Dump dump) throws IOException;
}
