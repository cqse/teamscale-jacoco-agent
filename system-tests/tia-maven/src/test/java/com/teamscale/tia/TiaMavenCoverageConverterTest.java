package com.teamscale.tia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Paths;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.junit.jupiter.api.Test;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.report.util.JsonUtils;
import com.teamscale.test.commons.SystemTestUtils;

/**
 * Tests the automatic conversion of .exec files to a testwise coverage report.
 */
public class TiaMavenCoverageConverterTest {

	/**
	 * The name of the project that the maven plugin is tested with.
	 */
	private static final String PROJECT_NAME = "maven-exec-file-project";

	/**
	 * Starts a maven process with the reuseForks flag set to "false" and tiaMode
	 * "exec-file". Checks if the coverage can be converted to a testwise coverage
	 * report afterward.
	 */
	@Test
	public void testMavenTia() throws Exception {
		File workingDirectory = new File(PROJECT_NAME);
		SystemTestUtils.runMavenTests(PROJECT_NAME);
		File testwiseCoverage = new File(
				Paths.get(workingDirectory.getAbsolutePath(), "target", "tia", "reports", "testwise-coverage-1.json")
						.toUri());
		TestwiseCoverageReport testwiseCoverageReport = JsonUtils
				.deserialize(FileSystemUtils.readFile(testwiseCoverage), TestwiseCoverageReport.class);
		assertNotNull(testwiseCoverageReport);
		assertAll(() -> {
			assertThat(testwiseCoverageReport.tests).extracting(test -> test.uniformPath).contains(
					"bar/TwoUnitTest/itBla()", "bar/TwoUnitTest/itFoo()", "bar/UnitTest/itBla()",
					"bar/UnitTest/itFoo()");
			assertThat(testwiseCoverageReport.tests).extracting(test -> test.result)
					.doesNotContain(ETestExecutionResult.FAILURE);
			assertThat(testwiseCoverageReport.tests).extracting(SystemTestUtils::getCoverageString).containsExactly(
					"SUT.java:3,6-7", "SUT.java:3,10-11", "", "SUT.java:3,6-7", "SUT.java:3,10-11", "");
		});
	}
}
