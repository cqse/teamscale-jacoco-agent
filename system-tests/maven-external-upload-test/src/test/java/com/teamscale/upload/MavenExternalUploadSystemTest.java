package com.teamscale.upload;

import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs several Maven projects' Surefire and Failsafe tests that produce coverage via the Jacoco Maven plugin
 * Checks that the Jacoco reports are correctly uploaded to a Teamscale instance.
 */
public class MavenExternalUploadSystemTest {

	/**
	 * This port must match what is configured for the -javaagent line in the corresponding POM of the Maven test
	 * project.
	 */
	private static final int FAKE_TEAMSCALE_PORT = 65432;
	private static TeamscaleMockServer teamscaleMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = new TeamscaleMockServer(FAKE_TEAMSCALE_PORT,
					"bar/UnitTest/utBla()", "bar/UnitTest/utFoo()",
					"bar/IntegIT/itBla()", "bar/IntegIT/itFoo()");
		}
		teamscaleMockServer.uploadedReports.clear();
	}

	@Test
	public void testMavenExternalUpload() throws Exception {
		SystemTestUtils.runMavenTests("root-project");
		assertThat(teamscaleMockServer.uploadedReports.size()).isEqualTo(4);
	}

}
