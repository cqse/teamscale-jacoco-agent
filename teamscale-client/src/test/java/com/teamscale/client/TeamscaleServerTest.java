package com.teamscale.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamscaleServerTest {

	@Test
	void testDefaultMessage() {
		TeamscaleServer server = new TeamscaleServer();
		server.partition = "Unit Test";
		server.revision = "rev123";

		String message = server.getMessage();
		String normalizedMessage = message.replaceAll("uploaded at .*", "uploaded at DATE")
				.replaceAll("hostname: .*", "hostname: HOST");
		assertEquals("Unit Test coverage uploaded at DATE\n\nuploaded from hostname: HOST\nfor revision: rev123",
				normalizedMessage);
	}

}