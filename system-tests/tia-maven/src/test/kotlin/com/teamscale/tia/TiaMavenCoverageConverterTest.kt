package com.teamscale.tia

import com.teamscale.client.JsonUtils.deserialize
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import com.teamscale.test.commons.SystemTestUtils.runMavenTests
import org.assertj.core.api.Assertions.assertThat
import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths


/**
 * Tests the automatic conversion of .exec files to a testwise coverage report.
 */
class TiaMavenCoverageConverterTest {
	/**
	 * Starts a maven process with the reuseForks flag set to "false" and tiaMode
	 * "exec-file". Checks if the coverage can be converted to a testwise coverage
	 * report afterward.
	 */
	@Test
	@Throws(Exception::class)
	fun testMavenTia() {
		runMavenTests(PROJECT_NAME)
		val testwiseCoverage = Paths.get(
			File(PROJECT_NAME).absolutePath,
			"target", "tia", "reports", "testwise-coverage-1.json"
		).toFile()
		val testwiseCoverageReport = deserialize<TestwiseCoverageReport>(
			FileSystemUtils.readFile(testwiseCoverage)
		)
		assertNotNull(testwiseCoverageReport)
		val tests = testwiseCoverageReport.tests
		assertThat(tests.map { it.uniformPath }).contains(
			"bar/TwoUnitTest/itBla()", "bar/TwoUnitTest/itFoo()", "bar/UnitTest/itBla()",
			"bar/UnitTest/itFoo()"
		)
		assertThat(tests.map { it.result })
			.doesNotContain(ETestExecutionResult.FAILURE)
		assertThat(tests.map { it.coverage }).containsExactly(
			"SUT.java:3,6-7", "SUT.java:3,10-11", "", "SUT.java:3,6-7", "SUT.java:3,10-11", ""
		)
	}

	companion object {
		/**
		 * The name of the project that the maven plugin is tested with.
		 */
		private const val PROJECT_NAME = "maven-exec-file-project"
	}
}
