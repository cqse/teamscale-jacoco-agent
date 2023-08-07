package com.teamscale.tia;

import org.assertj.core.api.Assertions;
import org.conqat.lib.commons.io.ProcessUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * Runs the agent with the HTTP server enabled and makes sure that the profiled application is shut down. This ensures
 * that the HTTP server is not blocking application shutdown with non-daemon threads.
 */
public class HttpServerShutdownSystemTest {

	@Test
	@Timeout(value = 10, unit = TimeUnit.SECONDS) // if the test exceeds the timeout, the shutdown didn't succeed
	public void testShutdown() throws Exception {
		String agentJar = System.getenv("AGENT_JAR");
		String sampleJar = System.getenv("SAMPLE_JAR");
		ProcessUtils.ExecutionResult result = ProcessUtils.execute(
				new ProcessBuilder("java", "-javaagent:" + agentJar + "=http-server-port=31223", "-jar", sampleJar));
		System.out.println(result.getStderr());
		System.out.println(result.getStdout());
		Assertions.assertThat(result.getReturnCode()).isEqualTo(0);
	}

}
