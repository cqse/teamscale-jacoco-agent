/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent;

import com.teamscale.jacoco.agent.store.IXmlStore;
import com.teamscale.jacoco.agent.store.UploadStoreException;
import com.teamscale.jacoco.agent.util.Benchmark;
import com.teamscale.jacoco.agent.util.Timer;
import com.teamscale.report.jacoco.JaCoCoXmlReportGenerator;
import com.teamscale.report.jacoco.dump.Dump;

import java.io.IOException;
import java.time.Duration;

import static com.teamscale.jacoco.agent.util.LoggingUtils.wrap;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and XML conversion based on a time
 * interval.
 */
public class Agent extends AgentBase {

	/** Converts binary data to XML. */
	private JaCoCoXmlReportGenerator generator;

	/** Regular dump task. */
	private Timer timer;

	/** Stores the XML files. */
	protected final IXmlStore store;

	/** Constructor. */
	/*package*/ Agent(AgentOptions options) throws IllegalStateException, UploadStoreException {
		super(options);

		store = options.createStore();
		logger.info("Storage method: {}", store.describe());

		generator = new JaCoCoXmlReportGenerator(options.getClassDirectoriesOrZips(),
				options.getLocationIncludeFilter(),
				options.duplicateClassFileBehavior(), wrap(logger));

		if (options.shouldDumpInIntervals()) {
			timer = new Timer(this::dumpReport, Duration.ofMinutes(options.getDumpIntervalInMinutes()));
			timer.start();
			logger.info("Dumping every {} minutes.", options.getDumpIntervalInMinutes());
		}
	}

	@Override
	protected void prepareShutdown() {
		if (timer != null) {
			timer.stop();
		}
		if (options.shouldDumpOnExit()) {
			dumpReport();
		}
	}

	/**
	 * Dumps the current execution data, converts it and writes it to the {@link #store}. Logs any errors, never throws
	 * an exception.
	 */
	private void dumpReport() {
		logger.debug("Starting dump");

		try {
			dumpReportUnsafe();
		} catch (Throwable t) {
			// we want to catch anything in order to avoid crashing the whole system under test
			logger.error("Dump job failed with an exception", t);
		}
	}

	private void dumpReportUnsafe() {
		Dump dump;
		try {
			dump = controller.dumpAndReset();
		} catch (JacocoRuntimeController.DumpException e) {
			logger.error("Dumping failed, retrying later", e);
			return;
		}

		String xml;
		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			xml = generator.convert(dump);
		} catch (IOException e) {
			logger.error("Converting binary dump to XML failed", e);
			return;
		}

		store.store(xml);
	}
}
