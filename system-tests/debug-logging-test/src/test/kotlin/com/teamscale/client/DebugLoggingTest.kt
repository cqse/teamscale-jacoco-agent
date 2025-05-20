package com.teamscale.client

import org.junit.jupiter.api.Test
import java.nio.file.Paths

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.io.path.exists
import kotlin.io.path.readLines

class DebugLoggingTest {
	@Test
	@Throws(Exception::class)
	fun systemTest() {
		assertTrue(LOG_DIRECTORY.exists())
		assertThat(LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log").readLines()).isNotEmpty()
	}

	companion object {
		private val LOG_DIRECTORY = Paths.get("logTest").resolve("logs")
	}
}
