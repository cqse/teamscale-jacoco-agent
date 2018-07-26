package eu.cqse.teamscale.test;

import org.jacoco.agent.rt.internal_c13123e.core.runtime.AgentOptions;

import java.io.File;
import java.lang.instrument.Instrumentation;

/** Wrapper around JaCoCo's PreMain to ensure the PreMain class name stays the same. */
public class PreMain {

	/** Entry point called by the JVM. */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions = new AgentOptions(options);
		agentOptions.setOutput(AgentOptions.OutputMode.file);
		new File(agentOptions.getDestfile()).delete();
		agentOptions.setAppend(true);
		agentOptions.setSessionId("");
		agentOptions.setDumpOnExit(false);
		org.jacoco.agent.rt.internal_c13123e.PreMain.premain(agentOptions.toString(), instrumentation);
	}
}
