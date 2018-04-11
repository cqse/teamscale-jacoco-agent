/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.time.Duration;

import org.jacoco.agent.rt.internal_8ff85ea.PreMain;

import eu.cqse.teamscale.jacoco.client.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.client.XmlReportGenerator;
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

	/** Command line options of the agent. */
	private final AgentOptions options;

	private final Timer timer;

	private final JacocoRuntimeController controller;
	private final XmlReportGenerator generator;
	private final TimestampedFileStore store;

	/** Constructor. */
	public Agent(AgentOptions options) {
		this.options = options;

		controller = new JacocoRuntimeController();
		generator = new XmlReportGenerator(options.classDirectoriesOrZips, options.locationIncludeFilters,
				options.locationExcludeFilters, options.shouldIgnoreDuplicateClassFiles);
		store = new TimestampedFileStore(options.outputDir);

		timer = new Timer(this::dump, Duration.ofMinutes(60));
	}

	private void dump() {
		try {
			Dump dump = controller.dumpAndReset();
			String xml = generator.convert(dump);
			store.store(xml);
		} catch (IllegalStateException | IOException e) {
			// TODO (FS) error handling
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final
	 * time.
	 */
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			dump();
			timer.stop();
		}));
	}

	/**
	 * 
	 */
	private void startDumpLoop() {
		timer.start();
	}

	/**
	 * Starts the JaCoCo Java agent.
	 */
	private static void startJacocoAgent(AgentOptions options, Instrumentation inst) throws Exception {
		PreMain.premain(options.createJacocoAgentOptions(), inst);
	}

}
