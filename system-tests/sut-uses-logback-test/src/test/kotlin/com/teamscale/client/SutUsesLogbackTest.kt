package com.teamscale.client

import org.assertj.core.api.Assertions.assertThat
import org.conqat.lib.commons.io.ProcessUtils
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class SutUsesLogbackTest {
	@Test
	@Throws(Exception::class)
	fun systemTest() {
		val result = ProcessUtils.execute(
			arrayOf("java", "-javaagent:$AGENT_JAR=debug=true", "-jar", "build/libs/app.jar")
		)

		assertThat(result.stdout).contains("This warning is to test logging in the SUT")
		assertThat(result.stdout).doesNotContainIgnoringCase("error")
		assertThat(result.stderr).isEmpty()

		val appLogFile = Paths.get("logTest/app.log")
		assertThat(appLogFile).exists()
		assertThat(appLogFile).content().contains("This warning is to test logging in the SUT")
	}

	companion object {
		private val AGENT_JAR = System.getProperty("agentJar")
	}
}
