package com.teamscale.client;

import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.Test;
import systemundertest.SystemUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures that the teamscale.properties file is successfully located and parsed.
 */
public class TeamscalePropertiesSystemTest {

	/** These ports must match what is configured for the -javaagent line in this project's build.gradle. */
	private static final int FAKE_TEAMSCALE_PORT = 63302;
	private static final int AGENT_PORT = 63301;

	@Test
	public void systemTest() throws Exception {
		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(FAKE_TEAMSCALE_PORT);

		new SystemUnderTest().foo();
		SystemTestUtils.dumpCoverage(AGENT_PORT);

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);
		teamscaleMockServer.shutdown();
	}

}
