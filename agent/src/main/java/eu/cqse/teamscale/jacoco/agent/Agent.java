/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.linebased.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.util.Timer;

import java.io.IOException;
import java.time.Duration;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and
 * XML conversion based on a time interval.
 */
public class Agent extends AgentBase {

    /** Converts binary data to XML. */
    private XmlReportGenerator generator;

    /** Regular dump task. */
    private final Timer timer;

    /** Constructor. */
    public Agent(AgentOptions options) {
        super(options);

        generator = new XmlReportGenerator(options.getClassDirectoriesOrZips(), options.getLocationIncludeFilter(),
                options.shouldIgnoreDuplicateClassFiles());

        timer = new Timer(this::dumpReport, Duration.ofMinutes(options.getDumpIntervalInMinutes()));
        timer.start();

        logger.info("Dumping every {} minutes.", options.getDumpIntervalInMinutes());
    }

    @Override
    protected void prepareShutdown() {
        timer.stop();
        dumpReport();
    }

    /**
     * Dumps the current execution data, converts it and writes it to the
     * {@link #store}. Logs any errors, never throws an exception.
     */
    private void dumpReport() {
        logger.debug("Starting dump");

        try {
            dumpReportUnsafe();
        } catch (Throwable t) {
            // we want to catch anything in order to avoid killing the regular job
            logger.error("Dump job failed with an exception. Retrying later", t);
        }
    }

    /**
     * Performs the actual dump but does not handle e.g. OutOfMemoryErrors.
     */
    private void dumpReportUnsafe() {
        Dump dump;
        try {
            dump = controller.dumpAndReset();
        } catch (JacocoRuntimeController.DumpException e) {
            logger.error("Dumping failed, retrying later", e);
            return;
        }

        String xml;
        try {
            xml = generator.convert(dump);
        } catch (IOException e) {
            logger.error("Converting binary dump to XML failed", e);
            return;
        }

        store.store(xml);
    }
}
