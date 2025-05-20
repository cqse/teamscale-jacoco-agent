package com.teamscale.tia.client

import com.teamscale.client.EReportFormat
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.dumpCoverage
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import systemundertest.SystemUnderTest

/**
 * Runs the system under test and then forces a dump of the agent to our [TeamscaleMockServer]. Checks the
 * resulting report to ensure the default excludes are applied.
 */
class DefaultExcludesSystemTest {
	@Test
	@Throws(Exception::class)
	fun systemTest() {
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog")
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF")

		val teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
			.withAuthentication("fake", "fake")
			.acceptingReportUploads()

		SystemUnderTest().foo()
		dumpCoverage(SystemTestUtils.AGENT_PORT)

		val report = teamscaleMockServer.getOnlyReport("part", EReportFormat.JACOCO)
		assertThat(report).doesNotContain("shadow", "junit", "eclipse", "apache", "javax", "slf4j", "com/sun")
		assertThat(report).contains("SystemUnderTest", "NotExcludedClass")
	}
}
