package com.teamscale.client

import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.dumpCoverage
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import systemundertest.SystemUnderTest

/**
 * Ensures that the teamscale.properties file is successfully located and parsed.
 */
class TeamscalePropertiesSystemTest {
	@Test
	@Throws(Exception::class)
	fun systemTest() {
		val teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
			.acceptingReportUploads()

		SystemUnderTest().foo()
		dumpCoverage(SystemTestUtils.AGENT_PORT)

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1)
		teamscaleMockServer.shutdown()
	}
}
