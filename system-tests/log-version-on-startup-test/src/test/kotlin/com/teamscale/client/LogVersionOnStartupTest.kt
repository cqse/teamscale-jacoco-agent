package com.teamscale.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.io.path.exists
import kotlin.io.path.readLines

/**
 * Ensures that the agent logs its version on startup at level INFO.
 */
class LogVersionOnStartupTest {
	@Test
	@Throws(Exception::class)
	fun systemTest() {
		assertTrue(LOG_DIRECTORY.exists())
		val logContent = LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log")
			.readLines()
			.joinToString("\n")
		assertThat(logContent).containsPattern(
			"INFO.*" + Pattern.quote(AGENT_VERSION)
		)
	}

	companion object {
		private val LOG_DIRECTORY = Paths.get("logTest").resolve("logs")
		private val AGENT_VERSION = System.getenv("AGENT_VERSION")
	}
}
