package com.teamscale.upload;

import com.teamscale.test.commons.ExternalReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.apache.commons.lang3.SystemUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.io.ProcessUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Runs several Maven projects' Surefire and Failsafe tests that produce coverage via the Jacoco Maven plugin. Checks
 * that the Jacoco reports are correctly uploaded to a Teamscale instance.
 */
public class MavenExternalUploadSystemTest {

	private static final String MAVEN_COVERAGE_UPLOAD_GOAL = "com.teamscale:teamscale-maven-plugin:upload-coverage";

	private static TeamscaleMockServer teamscaleMockServer = null;

	private static final String NESTED_MAVEN_PROJECT_NAME = "nested-project";

	private static final String FAILING_MAVEN_PROJECT_NAME = "failing-project";

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads();
		}
		teamscaleMockServer.uploadedReports.clear();
	}

	private ProcessUtils.ExecutionResult runCoverageUploadGoal(String projectPath) {
		File workingDirectory = new File(projectPath);
		String executable = "./mvnw";
		if (SystemUtils.IS_OS_WINDOWS) {
			executable = Paths.get(projectPath, "mvnw.cmd").toUri().getPath();
		}
		try {
			return ProcessUtils.execute(
					new ProcessBuilder(executable, MAVEN_COVERAGE_UPLOAD_GOAL).directory(workingDirectory));
		} catch (IOException e) {
			fail(String.valueOf(e));
		}
		return null;
	}

	@Test
	public void testMavenExternalUpload() throws Exception {
		SystemTestUtils.runMavenTests(NESTED_MAVEN_PROJECT_NAME);
		runCoverageUploadGoal(NESTED_MAVEN_PROJECT_NAME);
		assertThat(teamscaleMockServer.uploadedReports).hasSize(6);
		ExternalReport unitTests = teamscaleMockServer.uploadedReports.get(0);
		ExternalReport integrationTests = teamscaleMockServer.uploadedReports.get(3);
		assertThat(unitTests.getPartition()).isEqualTo("My Custom Unit Tests Partition");
		assertThat(integrationTests.getPartition()).isEqualTo("Integration Tests");
	}

	@Test
	public void testIncorrectJaCoCoConfiguration() throws IOException {
		SystemTestUtils.runMavenTests(FAILING_MAVEN_PROJECT_NAME);
		ProcessUtils.ExecutionResult result = runCoverageUploadGoal(FAILING_MAVEN_PROJECT_NAME);
		assertThat(result).isNotNull();
		assertThat(teamscaleMockServer.uploadedReports).isEmpty();
		assertThat(result.getStdout()).contains(
				String.format("Skipping upload for %s as %s is not configured to produce XML reports",
						FAILING_MAVEN_PROJECT_NAME, "org.jacoco:jacoco-maven-plugin"));
	}

	/**
	 * When no commit is given and no git repo is available, which is the usual fallback, a helpful error message should
	 * be shown (TS-40425).
	 */
	@Test
	public void testErrorMessageOnMissingCommit(@TempDir Path tmpDir) throws IOException {
		FileSystemUtils.copyFiles(new File("missing-commit-project"), tmpDir.toFile(), file -> true);
		tmpDir.resolve("mvnw").toFile().setExecutable(true);
		String projectPath = tmpDir.toAbsolutePath().toString();
		SystemTestUtils.runMavenTests(projectPath);
		ProcessUtils.ExecutionResult result = runCoverageUploadGoal(projectPath);
		assertThat(result).isNotNull();
		assertThat(result.getReturnCode()).isNotEqualTo(0);
		assertThat(teamscaleMockServer.uploadedReports).isEmpty();
		assertThat(result.getStdout()).contains("There is no <commit> configured in the pom.xml and it was not possible to determine the checked out commit");
	}

	@AfterAll
	public static void stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown();
	}

}
