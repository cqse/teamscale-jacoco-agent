/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.agent.rt.internal_c13123e.PreMain;

import eu.cqse.teamscale.jacoco.agent.AgentOptions.AgentOptionParseException;
import eu.cqse.teamscale.jacoco.agent.dump.Dump;
import eu.cqse.teamscale.jacoco.agent.dump.JacocoRuntimeController;
import eu.cqse.teamscale.jacoco.agent.dump.JacocoRuntimeController.DumpException;
import eu.cqse.teamscale.jacoco.agent.report.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.util.LoggingUtils;
import eu.cqse.teamscale.jacoco.agent.util.Timer;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and
 * XML conversion based on a time interval.
 */
public class Agent extends AgentBase {

	/**
	 * Entry point for the agent, called by the JVM.
	 */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		try {
			agentOptions = new AgentOptions(options);
		} catch (AgentOptionParseException e) {
			LoggingUtils.initializeDefaultLogging();
			LogManager.getLogger(Agent.class).fatal("Failed to parse agent options: " + e.getMessage(), e);
			System.err.println("Failed to parse agent options: " + e.getMessage());
			throw e;
		}

		LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		LogManager.getLogger(Agent.class).info("Starting JaCoCo's agent");
		PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		if (agentOptions.shouldUseHttpServerMode()) {
			ServerAgent agent = new ServerAgent(agentOptions);
			agent.startServer();
			agent.registerShutdownHook();
		} else {
			Agent agent = new Agent(agentOptions);
			agent.startDumpLoop();
			agent.registerShutdownHook();
		}
	}

	/** Regular dump task. */
	private final Timer timer;

	/** Converts binary data to XML. */
	private final XmlReportGenerator generator;

	/** Constructor. */
	public Agent(AgentOptions options) {
		super(options);

		generator = new XmlReportGenerator(options.getClassDirectoriesOrZips(), options.getLocationIncludeFilter(),
				options.shouldIgnoreDuplicateClassFiles());

		timer = new Timer(this::dump, Duration.ofMinutes(options.getDumpIntervalInMinutes()));

		logger.info("Dumping every {} minutes. Storage method: {}", options.getDumpIntervalInMinutes(),
				store.describe());
	}

	@Override
	protected String generateReport(Dump dump) throws IOException {
		return generator.convert(dump);
	}

	@Override
	protected void prepareShutdown() {
		dump();
		timer.stop();
	}

	/**
	 * Starts the regular {@link #dump()}.
	 */
	private void startDumpLoop() {
		timer.start();
	}

}
