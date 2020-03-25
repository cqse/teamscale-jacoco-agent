package com.teamscale.tia.client;

import org.junit.jupiter.api.Test;
import testframework.CustomTestFramework;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the custom test framework from src/main with our agent attached (the agent is configured in this project's
 * build.gradle). The custom test framework contains an integration via the tia-client against the {@link
 * TeamscaleMockServer}. Asserts that the resulting report looks as expected.
 * <p>
 * This ensures that our tia-client library doesn't break unexpectedly.
 */
public class SystemTest {

	/** These ports must match what is configured for the -javaagent line in this project's build.gradle. */
	private final int fakeTeamscalePort = 65432;
	private final int agentPort = 65433;

	@Test
	public void systemTest() throws Exception {
		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(fakeTeamscalePort, "testFoo", "testBar");
		CustomTestFramework customTestFramework = new CustomTestFramework(agentPort);
		customTestFramework.runTestsWithTia();

		assertThat(teamscaleMockServer.uploadedReports).containsExactly(
				"{\"tests\":[" +
						// testBar
						"{\"message\":\"Incorrect return value for bar\",\"paths\":[" +
						"{\"files\":[{\"coveredLines\":\"3,10\",\"fileName\":\"SystemUnderTest.java\"}]," +
						"\"path\":\"systemundertest\"}]," +
						"\"result\":\"FAILURE\",\"sourcePath\":\"testBar\",\"uniformPath\":\"testBar\"}," +
						// testFoo
						"{\"message\":\"\",\"paths\":[" +
						"{\"files\":[{\"coveredLines\":\"3,6\",\"fileName\":\"SystemUnderTest.java\"}]," +
						"\"path\":\"systemundertest\"}]," +
						"\"result\":\"PASSED\",\"sourcePath\":\"testFoo\",\"uniformPath\":\"testFoo\"}]}");
	}

}
