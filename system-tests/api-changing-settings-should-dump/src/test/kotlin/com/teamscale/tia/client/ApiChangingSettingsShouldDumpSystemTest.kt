package com.teamscale.tia.client

import com.teamscale.client.EReportFormat
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.changePartition
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import systemundertest.SystemUnderTest

/**
 * Runs the system under test and then changes the partition of the agent. This should cause a dump to our
 * [TeamscaleMockServer].
 */
class ApiChangingSettingsShouldDumpSystemTest {
	@Test
	@Throws(Exception::class)
	fun systemTest() {
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog")
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF")

		val teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads()

		SystemUnderTest().foo()
		changePartition(SystemTestUtils.AGENT_PORT, "some_other_value")

		val session = teamscaleMockServer.onlySession
		assertThat(session.partition).isEqualTo("partition_before_change")
		assertThat(session.getOnlyReport(EReportFormat.JACOCO)).contains(
			"<line nr=\"$METHOD_FOO_COVERABLE_LINE\" mi=\"0\""
		)
	}

	companion object {
		private const val METHOD_FOO_COVERABLE_LINE = 4
	}
}
