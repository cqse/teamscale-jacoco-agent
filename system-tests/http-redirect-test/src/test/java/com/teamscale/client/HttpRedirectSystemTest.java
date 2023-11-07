package com.teamscale.client;

import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.Test;
import systemundertest.SystemUnderTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the system under test and then forces a dump of the agent to a {@link RedirectMockServer}. This sends redirects
 * to our {@link TeamscaleMockServer}. Checks that the agent respects and follows these redirects.
 */
public class HttpRedirectSystemTest {

	private static final int REDIRECT_PORT = Integer.getInteger("redirectPort");

	@Test
	public void systemTest() throws Exception {
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

		RedirectMockServer redirectMockServer = new RedirectMockServer(REDIRECT_PORT, SystemTestUtils.TEAMSCALE_PORT);
		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads();

		new SystemUnderTest().foo();
		SystemTestUtils.dumpCoverage(SystemTestUtils.AGENT_PORT);

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);
		checkCustomUserAgent(teamscaleMockServer);

		redirectMockServer.shutdown();
		teamscaleMockServer.shutdown();
	}

	private void checkCustomUserAgent(TeamscaleMockServer teamscaleMockServer) {
		Set<String> collectedUserAgents = teamscaleMockServer.collectedUserAgents;
		assertThat(collectedUserAgents).containsExactly(TeamscaleServiceGenerator.USER_AGENT);
	}

}
