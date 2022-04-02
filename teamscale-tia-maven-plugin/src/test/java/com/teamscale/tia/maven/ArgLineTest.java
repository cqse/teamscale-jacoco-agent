package com.teamscale.tia.maven;

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
		String secondArgLine = argLine.prependTo(firstArgLine);

		assertEquals(firstArgLine, secondArgLine);
	}

	@Test
	public void secondInvocationOverridesFirst() {
		String firstArgLine = new ArgLine(null, "info", Paths.get("agent.jar"), Paths.get("agent.properties"),
				Paths.get("agent.log")).prependTo("");
		String secondArgLine = new ArgLine(null, "info", Paths.get("agent2.jar"), Paths.get("agent.properties"),
				Paths.get("agent.log")).prependTo(firstArgLine);

		assertTrue(secondArgLine.contains("agent2.jar"));
	}

	@Test
	public void preservesUnrelatedAgents() {
		String argLine = new ArgLine(null, "info", Paths.get("agent.jar"), Paths.get("agent.properties"),
				Paths.get("agent.log")).prependTo("-javaagent:someother.jar");

		assertTrue(argLine.matches(".*agent\\.jar.*someother\\.jar.*"));
	}

}