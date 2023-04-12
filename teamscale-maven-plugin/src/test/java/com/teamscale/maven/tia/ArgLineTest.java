package com.teamscale.maven.tia;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgLineTest {

	@Test
	public void isIdempotent() {
		ArgLine argLine = new ArgLine(null, "info", Paths.get("agent.jar"), Paths.get("agent.properties"),
				Paths.get("agent.log"));
		String firstArgLine = argLine.prependTo("");
		String secondArgLine = argLine.prependTo(ArgLine.removePreviousTiaAgent(firstArgLine));

		assertEquals(firstArgLine, secondArgLine);
	}

	@Test
	public void testNullOriginalArgLine() {
		ArgLine argLine = new ArgLine(null, "info", Paths.get("agent.jar"), Paths.get("agent.properties"),
				Paths.get("agent.log"));
		String newArgLine = argLine.prependTo(null);

		assertTrue(newArgLine.startsWith("-Dteamscale.markstart"));
		assertTrue(newArgLine.endsWith("-Dteamscale.markend"));
	}

	@Test
	public void preservesUnrelatedAgents() {
		String argLine = "-javaagent:someother.jar";
		String newArgLine = ArgLine.removePreviousTiaAgent(argLine);

		assertEquals(argLine, newArgLine);
	}

}