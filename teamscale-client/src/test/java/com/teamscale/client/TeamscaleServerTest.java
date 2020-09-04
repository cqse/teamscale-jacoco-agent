package com.teamscale.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamscaleServerTest {

	@Test
	void testDefaultMessage() {
		TeamscaleServer server = new TeamscaleServer();
		server.partition = "Unit Test";
		server.revision = "rev123";

		String message = server.getMessage();
		String normalizedMessage = message.replaceAll("at .* for", "at DATE for");
		assertEquals("Unit Test coverage uploaded at DATE for revision rev123", normalizedMessage);
	}

}