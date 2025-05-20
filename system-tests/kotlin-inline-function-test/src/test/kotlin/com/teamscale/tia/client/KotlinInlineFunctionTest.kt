package com.teamscale.tia.client

import com.teamscale.client.EReportFormat
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.dumpCoverage
import com.teamscale.test.commons.TeamscaleMockServer
import foo.main
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Runs the system under test and then forces a dump of the agent to our [TeamscaleMockServer].
 * Checks that the correct line numbers are generated for Kotlin inline functions.
 */
class KotlinInlineFunctionTest {
	@Test
	@Throws(Exception::class)
	fun systemTest() {
		val teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads()
		main()
		dumpCoverage(SystemTestUtils.AGENT_PORT)

		val report = teamscaleMockServer.getOnlyReport("part", EReportFormat.JACOCO)
		Assertions.assertThat(report).doesNotContain("<line nr=\"8\"")
		Assertions.assertThat(report).contains("nr=\"4\" mi=\"0\" ci=\"21\"")
	}
}
