package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.util.LoggingUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.instrument.Instrumentation;

/** Container class for the premain entry point for the agent. */
public class PreMain {

	/** Entry point for the agent, called by the JVM. */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		DelayedLogger delayedLogger = new DelayedLogger();
		try {
			agentOptions = AgentOptionsParser.parse(options, delayedLogger);
		} catch (AgentOptionParseException e) {
			LoggingUtils.initializeDefaultLogging();
			Logger logger = LogManager.getLogger(PreMain.class);
			delayedLogger.logTo(logger);
			logger.fatal("Failed to parse agent options: " + e.getMessage(), e);
			System.err.println("Failed to parse agent options: " + e.getMessage());
			throw e;
		}

		LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		Logger logger = LogManager.getLogger(Agent.class);
		delayedLogger.logTo(logger);
		logger.info("Starting JaCoCo's agent");
		org.jacoco.agent.rt.internal_c13123e.PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentOptions.createAgent();
		agent.registerShutdownHook();
	}
}
