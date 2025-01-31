package com.teamscale.maven.tia

import com.teamscale.maven.tia.ArgLine.Companion.removePreviousTiaAgent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths

internal class ArgLineTest {
	@Test
	fun isIdempotent() {
		val argLine = ArgLine(
			null, "info", Paths.get("agent.jar"), Paths.get("agent.properties"),
			Paths.get("agent.log")
		)
		val firstArgLine = argLine.prependTo("")
		val secondArgLine = argLine.prependTo(removePreviousTiaAgent(firstArgLine))

		assertEquals(firstArgLine, secondArgLine)
	}

	@Test
	fun testNullOriginalArgLine() {
		val argLine = ArgLine(
			null, "info", Paths.get("agent.jar"), Paths.get("agent.properties"),
			Paths.get("agent.log")
		)
		val newArgLine = argLine.prependTo(null)

		assertTrue(newArgLine.startsWith("-Dteamscale.markstart"))
		assertTrue(newArgLine.endsWith("-Dteamscale.markend"))
	}

	@Test
	fun preservesUnrelatedAgents() {
		val argLine = "-javaagent:someother.jar"
		val newArgLine = removePreviousTiaAgent(argLine)

		assertEquals(argLine, newArgLine)
	}
}