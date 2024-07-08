package com.teamscale.tia.client;

import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import foo.MainKt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the system under test and then forces a dump of the agent to our {@link TeamscaleMockServer}.
 * Checks that the correct line numbers are generated for Kotlin inline functions.
 */
public class KotlinInlineFunctionTest {

	@Test
	public void systemTest() throws Exception {
		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads();
		MainKt.main();
		SystemTestUtils.dumpCoverage(SystemTestUtils.AGENT_PORT);

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);
		String report = teamscaleMockServer.uploadedReports.get(0).getReportString();
		assertThat(report).doesNotContain("<line nr=\"8\"");
		assertThat(report).contains("nr=\"4\" mi=\"0\" ci=\"21\"");
	}

}
