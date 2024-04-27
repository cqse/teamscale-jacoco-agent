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

	@Test
	public void systemTest() throws Exception {
		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads();

		new SystemUnderTest().foo();
		SystemTestUtils.dumpCoverage(SystemTestUtils.AGENT_PORT);

		assertThat(teamscaleMockServer.getUploadedReports()).hasSize(1);
		teamscaleMockServer.shutdown();
	}

}
