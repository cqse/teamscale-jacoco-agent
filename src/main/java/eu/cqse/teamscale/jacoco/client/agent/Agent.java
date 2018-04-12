/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.agent.rt.internal_8ff85ea.PreMain;

import eu.cqse.teamscale.jacoco.client.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.client.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.client.agent.JacocoRuntimeController.DumpException;
import eu.cqse.teamscale.jacoco.client.util.Timer;
import eu.cqse.teamscale.jacoco.client.watch.IJacocoController.Dump;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and
 * XML conversion based on a time interval.
 */
public class Agent {

	/**
	 * Entry point for the agent, called by the JVM.
	 */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions = new AgentOptions(options);

		// start the JaCoCo agent
		PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		Agent agent = new Agent(agentOptions);
		agent.startDumpLoop();
		agent.registerShutdownHook();
	}

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** Regular dump task. */
	private final Timer timer;

	/** Controls the JaCoCo runtime. */
	private final JacocoRuntimeController controller;

	/** Converts binary data to XML. */
	private final XmlReportGenerator generator;

	/** Writes XML to the file system. */
	private final TimestampedFileStore store;

	/** Constructor. */
	public Agent(AgentOptions options) {
		controller = new JacocoRuntimeController();
		generator = new XmlReportGenerator(options.getClassDirectoriesOrZips(), options.getLocationIncludeFilters(),
				options.getLocationExcludeFilters(), options.isShouldIgnoreDuplicateClassFiles());
		store = new TimestampedFileStore(options.getOutputDir());

		timer = new Timer(this::dump, Duration.ofMinutes(options.getDumpIntervalInMinutes()));
	}

	/**
	 * Dumps the current execution data, converts it and writes it to the
	 * {@link #store}.
	 */
	private void dump() {
		Dump dump;
		try {
			dump = controller.dumpAndReset();
		} catch (DumpException e) {
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

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final
	 * time.
	 */
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			dump();
			timer.stop();
			System.err.println("CQSE JaCoCo agent successfully shut down.");
		}));
	}

	/**
	 * Starts the regular {@link #dump()}.
	 */
	private void startDumpLoop() {
		timer.start();
	}

}
