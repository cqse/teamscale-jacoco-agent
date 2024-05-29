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

	private static final int METHOD_FOO_COVERABLE_LINE = 7;
	private static final int METHOD_BAR_COVERABLE_LINE = 11;

	@Test
	public void systemTest() throws Exception {
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(
				SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads();

		new SystemUnderTest().foo();
		SystemTestUtils.changePartition(SystemTestUtils.AGENT_PORT, "some_other_value");

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);
		assertThat(teamscaleMockServer.uploadedReports.get(0).getPartition()).isEqualTo("partition_before_change");
		String report = teamscaleMockServer.uploadedReports.get(0).getReportString();
		// ensure that only the foo() method was covered, not the bar() method
		assertThat(report).contains(coveredLine(METHOD_FOO_COVERABLE_LINE))
				.contains(missedLine(METHOD_BAR_COVERABLE_LINE, 2));
	}

	private String missedLine(int lineNumber, int numberOfProbes) {
		return "<line nr=\"" + lineNumber + "\" mi=\"" + numberOfProbes + "\"";
	}

	private String coveredLine(int lineNumber) {
		return "<line nr=\"" + lineNumber + "\" mi=\"0\"";
	}

}
