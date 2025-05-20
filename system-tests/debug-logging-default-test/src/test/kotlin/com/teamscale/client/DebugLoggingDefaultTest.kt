package com.teamscale.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readLines

class DebugLoggingDefaultTest {

	@Test
	fun systemTest() {
		val logDirectory = searchForAgentTempDirectory().resolve("logs")
		assertThat(logDirectory.exists()).isTrue()
		assertThat(logDirectory.resolve("teamscale-jacoco-agent.log").readLines()).isNotEmpty()
	}

	private fun searchForAgentTempDirectory() =
		Paths.get(System.getProperty("java.io.tmpdir"))
			.toFile().walk().maxDepth(1)
			.firstOrNull { it.name.contains("teamscale-java-profiler") }?.toPath()
			?: throw AssertionError("Could not locate agent temp directory")
}
