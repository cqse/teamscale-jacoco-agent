package com.teamscale.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Paths
import kotlin.io.path.readLines

class DoubleStartAgentTest {
	@Test
	@Throws(IOException::class)
	fun systemTest() {
		assertThat(LOG_DIRECTORY).exists()
		val lines = LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log").readLines()
		val agentStartLines = lines.filter { it.contains("Starting Teamscale Java Profiler") }
		assertThat(agentStartLines).hasSize(1)
	}

	companion object {
		private val LOG_DIRECTORY = Paths.get("logTest").resolve("logs")
	}
}
