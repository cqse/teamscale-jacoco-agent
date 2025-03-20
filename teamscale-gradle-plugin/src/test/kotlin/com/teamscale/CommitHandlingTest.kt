package com.teamscale

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CommitHandlingTest : TeamscalePluginTestBase() {

	@BeforeEach
	fun init() {
		rootProject.withSingleProject()
		rootProject.defaultProjectSetup()
		rootProject.withServerConfig()
		rootProject.defineTestTasks()
		rootProject.defineUploadTask()
	}

	@Test
	fun `prefer using branch and timestamp to upload reports when provided manually`() {
		rootProject.withBranchAndTimestamp()

		val build = runExpectingError(
			"--continue",
			"clean",
			"unitTest",
			"-Dimpacted",
			"-DrunAllTests",
			"unitTestReportUpload"
		)
		assertThat(teamscaleMockServer.onlySession.commit).contains("null:myRepoId, master:1544512967526")
	}

	@Test
	fun `upload reports to repo and revision when timestamp is not provided manually`() {
		val build = runExpectingError(
			"--continue",
			"clean",
			"unitTest",
			"-Dimpacted",
			"-DrunAllTests",
			"unitTestReportUpload"
		)
		assertThat(teamscaleMockServer.onlySession.commit).contains("abcd1337:myRepoId, null")
	}

	@Test
	fun `prefer using branch and timestamp to retrieve impacted tests when provided manually`() {
		rootProject.withBranchAndTimestamp()

		val build = run(
			"--continue",
			"clean",
			"unitTest",
			"-Dimpacted",
			"unitTestReportUpload"
		)
		assertThat(teamscaleMockServer.impactedTestCommits).contains("null:myRepoId, master:1544512967526")
	}

	@Test
	fun `use repo and revision to retrieve impacted tests when timestamp is not provided manually`() {
		val build = run(
			"--continue",
			"clean",
			"unitTest",
			"-Dimpacted",
			"unitTestReportUpload"
		)
		assertThat(teamscaleMockServer.impactedTestCommits).contains("abcd1337:myRepoId, null")
	}
}
