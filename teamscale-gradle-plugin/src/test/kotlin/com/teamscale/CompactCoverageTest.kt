package com.teamscale

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


/**
 * Tests the CompactCoverageReport task.
 */
class CompactCoverageTest : TeamscalePluginTestBase() {

	@BeforeEach
	fun init() {
		rootProject.withSingleProject()
		rootProject.defaultProjectSetup()
	}

	@Test
	fun `CompactCoverageReport produces report`() {
		rootProject.buildFile.appendText(
			"""

tasks.register('compactCoverageReport', com.teamscale.CompactCoverageReport) {
	executionData(tasks.test)
	sourceSets(sourceSets.main)
}
		""".trimIndent()
		)

		assertThat(run("clean", "test", "compactCoverageReport").output).contains("SUCCESS")
		assertThat(rootProject.buildDir.resolve("reports/compact-coverage/compactCoverageReport/compact-coverage.json")).content()
			.contains("\"filePath\":\"com/example/project/Calculator.java\",\"fullyCoveredLines\":\"13,16,20-22\"")
			.contains("\"filePath\":\"com/example/project/Fibonacci.java\",\"fullyCoveredLines\":\"5,7-8,10,13\"")
	}
}
