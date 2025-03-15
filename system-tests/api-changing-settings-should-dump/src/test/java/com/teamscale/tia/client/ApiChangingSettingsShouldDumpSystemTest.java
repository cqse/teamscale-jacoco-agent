package com.teamscale.tia.client;

import com.teamscale.client.EReportFormat;
import com.teamscale.test.commons.Session;
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

	private static final int METHOD_FOO_COVERABLE_LINE = 7;

	@Test
	public void systemTest() throws Exception {
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(
				SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads();

		new SystemUnderTest().foo();
		SystemTestUtils.changePartition(SystemTestUtils.AGENT_PORT, "some_other_value");

		Session session = teamscaleMockServer.getOnlySession();
		assertThat(session.getPartition()).isEqualTo("partition_before_change");
		assertThat(session.getOnlyReport(EReportFormat.JACOCO)).contains(coveredLine(METHOD_FOO_COVERABLE_LINE));
	}

	private String coveredLine(int lineNumber) {
		return "<line nr=\"" + lineNumber + "\" mi=\"0\"";
	}

}
