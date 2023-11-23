package com.teamscale.tia;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import org.conqat.lib.commons.io.ProcessUtils;
import org.conqat.lib.commons.system.SystemUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the automatic conversion of .exec files to a testwise coverage report.
 */
public class TiaMavenCoverageConverterTest {

	/**
	 * The name of the project that the maven plugin is tested with.
	 */
	private static final String PROJECT_NAME = "maven-exec-file-project";
	private final JsonAdapter<TestwiseCoverageReport> testwiseCoverageReportJsonAdapter = new Moshi.Builder().build()
			.adapter(TestwiseCoverageReport.class);

	/**
	 * Starts a maven process with the reuseForks flag set to "false" and tiaMode "exec-file". Checks if the coverage
	 * can be converted to a testwise coverage report afterward.
	 */
	@Test
	public void testMavenTia() throws Exception {
		File workingDirectory = new File(PROJECT_NAME);
		String executable = "./mvnw";
		if (SystemUtils.isWindows()) {
			executable = Paths.get(PROJECT_NAME, "mvnw.cmd").toUri().getPath();
		}

		ProcessUtils.ExecutionResult result = ProcessUtils.execute(
				new ProcessBuilder(executable, "clean", "test",
						"teamscale-maven-plugin:testwise-coverage-converter").directory(
						workingDirectory));
		assertThat(result.terminatedByTimeoutOrInterruption()).isFalse();
		System.out.println(result.getStdout());
		File testwiseCoverage = new File(
				Paths.get(workingDirectory.getAbsolutePath(), "target", "tia", "reports", "testwise-coverage-1.json")
						.toUri());
		TestwiseCoverageReport testwiseCoverageReport = testwiseCoverageReportJsonAdapter.fromJson(
				new String(Files.readAllBytes(
						testwiseCoverage.toPath())));
		assertNotNull(testwiseCoverageReport);
		assertAll(() -> {
			assertThat(testwiseCoverageReport.tests).extracting(test -> test.uniformPath)
					.contains("bar/TwoUnitTest/itBla()", "bar/TwoUnitTest/itFoo()", "bar/UnitTest/itBla()",
							"bar/UnitTest/itFoo()");
			assertThat(testwiseCoverageReport.tests).extracting(test -> test.result)
					.doesNotContain(ETestExecutionResult.FAILURE);
			assertThat(testwiseCoverageReport.tests).extracting(SystemTestUtils::getCoverageString)
					.containsExactly("SUT.java:3,6-7",
							"SUT.java:3,10-11",
							"",
							"SUT.java:3,6-7",
							"SUT.java:3,10-11",
							"");
		});
	}
}
