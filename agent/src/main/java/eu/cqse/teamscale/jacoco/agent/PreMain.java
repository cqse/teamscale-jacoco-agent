package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.util.LoggingUtils;
import org.apache.logging.log4j.LogManager;

import java.lang.instrument.Instrumentation;

public class PreMain {

	/**
	 * Entry point for the agent, called by the JVM.
	 */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		try {
			agentOptions = new AgentOptions(options);
		} catch (AgentOptions.AgentOptionParseException e) {
			LoggingUtils.initializeDefaultLogging();
			LogManager.getLogger(PreMain.class).fatal("Failed to parse agent options: " + e.getMessage(), e);
			System.err.println("Failed to parse agent options: " + e.getMessage());
			throw e;
		}

		LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		LogManager.getLogger(Agent.class).info("Starting JaCoCo's agent");
		org.jacoco.agent.rt.internal_c13123e.PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentOptions.createAgent();
		agent.registerShutdownHook();
	}
}
