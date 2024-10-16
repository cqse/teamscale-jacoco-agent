package com.teamscale.client;

import com.teamscale.test.commons.TeamscaleMockServer;
import org.assertj.core.api.Assertions;
import org.conqat.lib.commons.io.ProcessUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures that the teamscale.properties file is successfully located and used to retrieve the configuration from
 * Teamscale.
 */
public class TeamscaleProfilerConfigurationSystemTest {

	/** These ports must match what is configured for the -javaagent line in this project's build.gradle. */
	private static final int FAKE_TEAMSCALE_PORT = 64100;

	@Test
	public void systemTestRetrieveConfig() throws Exception {
		ProfilerConfiguration profilerConfiguration = new ProfilerConfiguration();
		profilerConfiguration.configurationId = "my-config";
		profilerConfiguration.configurationOptions = "teamscale-partition=foo\nteamscale-project=p\nteamscale-commit=master:12345";
		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(FAKE_TEAMSCALE_PORT).acceptingReportUploads()
				.withProfilerConfiguration(profilerConfiguration);

		String agentJar = System.getenv("AGENT_JAR");
		String sampleJar = System.getenv("SYSTEM_UNDER_TEST_JAR");
		ProcessUtils.ExecutionResult result = ProcessUtils.execute(
				new ProcessBuilder("java", "-javaagent:" + agentJar + "=config-id=my-config", "-jar", sampleJar));
		System.out.println(result.getStderr());
		System.out.println(result.getStdout());
		Assertions.assertThat(result.getReturnCode()).isEqualTo(0);

		assertThat(teamscaleMockServer.uploadedReports).element(0).extracting("partition").isEqualTo("foo");

		teamscaleMockServer.shutdown();

		assertThat(teamscaleMockServer.getProfilerEvents()).as("We expect a sequence of interactions with the mock. " +
				"Note that unexpected interactions can be caused by old agent instances that have not been killed properly.") //
				.containsExactly("Profiler registered and requested configuration my-config",
						"Profiler 123 sent logs",
						"Profiler 123 sent heartbeat",
				"Profiler 123 unregistered");
	}

	/**
	 * Tests that the system under test does start up normally after the 2 minutes of timeout elapsed when Teamscale is
	 * not available.
	 */
	@Test
	public void systemTestLenientFailure() throws Exception {
		String agentJar = System.getenv("AGENT_JAR");
		String sampleJar = System.getenv("SYSTEM_UNDER_TEST_JAR");
		ProcessUtils.ExecutionResult result = ProcessUtils.execute(
				new ProcessBuilder("java", "-javaagent:" + agentJar + "=config-id=some-config", "-jar", sampleJar));
		System.out.println(result.getStderr());
		System.out.println(result.getStdout());
		Assertions.assertThat(result.getReturnCode()).isEqualTo(0);

		assertThat(result.getStdout()).contains("Production code");
	}

}
