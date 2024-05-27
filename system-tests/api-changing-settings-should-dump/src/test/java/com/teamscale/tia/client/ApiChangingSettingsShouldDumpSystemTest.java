package com.teamscale.tia.client;

import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.Test;
import systemundertest.SystemUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the system under test and then changes the partition of the agent. This should cause a dump to our
 * {@link TeamscaleMockServer}.
 */
public class ApiChangingSettingsShouldDumpSystemTest {

	@Test
	public void systemTest() throws Exception {
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(
				SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads();

		new SystemUnderTest().foo();
		SystemTestUtils.changePartition(SystemTestUtils.AGENT_PORT, "some_other_value");

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);
		String report = teamscaleMockServer.uploadedReports.get(0).getReportString();
		// ensure that only the foo() method was covered, not the bar() method
		assertThat(report).contains("<line nr=\"7\" mi=\"0\"").contains("<line nr=\"11\" mi=\"2\"");
	}

}
