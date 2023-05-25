package com.teamscale.upload;

import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.conqat.lib.commons.io.ProcessUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

	private static final String MAVEN_PROJFECT_PATH = "root-project";

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = new TeamscaleMockServer(FAKE_TEAMSCALE_PORT,
					"bar/UnitTest/utBla()", "bar/UnitTest/utFoo()",
					"bar/IntegIT/itBla()", "bar/IntegIT/itFoo()");
		}
		teamscaleMockServer.uploadedReports.clear();
	}

	private void runCoverageUploadGoal() {
		File workingDirectory = new File(MavenExternalUploadSystemTest.MAVEN_PROJFECT_PATH);
		try {
			ProcessUtils.execute(new ProcessBuilder("./mvnw", "com.teamscale:teamscale-maven-plugin:upload-coverage").directory(workingDirectory));
		} catch (IOException e) {
			fail(String.valueOf(e));
		}
	}

	@Test
	public void testMavenExternalUpload() throws Exception {
		SystemTestUtils.runMavenTests(MAVEN_PROJFECT_PATH);
		runCoverageUploadGoal();
		assertThat(teamscaleMockServer.uploadedReports.size()).isEqualTo(3);
		TestwiseCoverageReport report = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0);
		assertThat(report.tests.size()).isEqualTo(2);
	}

}
