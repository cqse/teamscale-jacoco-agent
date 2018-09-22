/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.util.Benchmark;
import eu.cqse.teamscale.jacoco.util.LoggingUtils;
import eu.cqse.teamscale.jacoco.util.Timer;
import eu.cqse.teamscale.report.jacoco.JaCoCoXmlReportGenerator;
import eu.cqse.teamscale.report.jacoco.dump.Dump;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.time.Duration;

import static eu.cqse.teamscale.client.EReportFormat.JACOCO;
import static eu.cqse.teamscale.jacoco.util.LoggingUtils.wrap;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and
 * XML conversion based on a time interval.
 */
public class Agent extends AgentBase {

	/** Converts binary data to XML. */
	private JaCoCoXmlReportGenerator generator;

	/** Regular dump task. */
	private final Timer timer;

	/** Entry point for the agent, called by the JVM. */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		try {
			agentOptions = AgentOptionsParser.parse(options);
		} catch (AgentOptionParseException e) {
			LoggingUtils.initializeDefaultLogging();
			LoggingUtils.getLogger(Agent.class).error("Failed to parse agent options: " + e.getMessage(), e);
			System.err.println("Failed to parse agent options: " + e.getMessage());
			throw e;
		}

		LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		LoggingUtils.getLogger(Agent.class).info("Starting JaCoCo's agent");
		org.jacoco.agent.rt.internal_c13123e.PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentOptions.createAgent();
		agent.registerShutdownHook();
	}

	/** Constructor. */
	/*package*/ Agent(AgentOptions options) throws IllegalStateException {
		super(options);

		generator = new JaCoCoXmlReportGenerator(options.getClassDirectoriesOrZips(),
				options.getLocationIncludeFilter(),
				options.shouldIgnoreDuplicateClassFiles(), wrap(logger));

		timer = new Timer(this::dumpReport, Duration.ofMinutes(options.getDumpIntervalInMinutes()));
		timer.start();

		logger.info("Dumping every {} minutes.", options.getDumpIntervalInMinutes());
	}

	@Override
	protected void prepareShutdown() {
		timer.stop();
		dumpReport();
	}

	@Override
	protected void dumpReportUnsafe() {
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

		store.store(xml, JACOCO);
	}
}
