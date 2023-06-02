package com.teamscale.upload;

import com.teamscale.test.commons.ExternalReport;
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
			teamscaleMockServer = new TeamscaleMockServer(FAKE_TEAMSCALE_PORT);
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
		assertThat(teamscaleMockServer.uploadedReports.size()).isEqualTo(2);
		ExternalReport unitTests = teamscaleMockServer.uploadedReports.get(0);
		ExternalReport integrationTests = teamscaleMockServer.uploadedReports.get(1);
		assertThat(unitTests.getPartition()).isEqualTo("My Custom Unit Tests Partition");
		assertThat(integrationTests.getPartition()).isEqualTo("Integration Tests");
	}

}
