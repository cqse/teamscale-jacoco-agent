package com.teamscale.client

import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.dumpCoverage
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import systemundertest.SystemUnderTest

class ArtifactoryGitPropertiesDetectionTest {
	private var artifactoryMockServer: ArtifactoryMockServer? = null

	@BeforeEach
	fun startFakeTeamscaleServer() {
		artifactoryMockServer = ArtifactoryMockServer(FAKE_ARTIFACTORY_PORT)
	}

	@AfterEach
	fun shutdownFakeTeamscaleServer() {
		artifactoryMockServer?.shutdown()
	}

	@Test
	@Throws(Exception::class)
	fun systemTest() {
		SystemUnderTest.foo()

		dumpCoverage(SystemTestUtils.AGENT_PORT)

		Assertions.assertThat(
			artifactoryMockServer?.uploadedReports
		).hasSize(1)
		val reports = artifactoryMockServer?.uploadedReports
		Assertions.assertThat(reports).hasSize(1)
		// Ensure infos from src/main/resources/git.properties are picked up
		Assertions.assertThat(reports?.getFirst(0))
			.startsWith("uploads/master/1645713803000-86f9d655bf8a204d98bc3542e0d15cea38cc7c74/")
	}

	companion object {
		private val FAKE_ARTIFACTORY_PORT: Int = Integer.getInteger("artifactoryPort")
	}
}
