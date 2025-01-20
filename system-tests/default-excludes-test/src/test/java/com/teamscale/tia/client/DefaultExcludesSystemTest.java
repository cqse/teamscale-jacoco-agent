package com.teamscale.tia.client;

import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.Test;
import systemundertest.SystemUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the system under test and then forces a dump of the agent to our {@link TeamscaleMockServer}. Checks the
 * resulting report to ensure the default excludes are applied.
 */
public class DefaultExcludesSystemTest {

	@Test
	public void systemTest() throws Exception {
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
				.withAuthentication("fake", "fake")
				.acceptingReportUploads();

		new SystemUnderTest().foo();
		SystemTestUtils.dumpCoverage(SystemTestUtils.AGENT_PORT);

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);
		String report = teamscaleMockServer.uploadedReports.get(0).reportString;
		assertThat(report).doesNotContain("shadow");
		assertThat(report).doesNotContain("junit");
		assertThat(report).doesNotContain("eclipse");
		assertThat(report).doesNotContain("apache");
		assertThat(report).doesNotContain("javax");
		assertThat(report).doesNotContain("slf4j");
		assertThat(report).doesNotContain("com/sun");
		assertThat(report).contains("SystemUnderTest");
		assertThat(report).contains("NotExcludedClass");
	}

}
