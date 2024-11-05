package com.teamscale.client

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TeamscaleServerTest {
	@Test
	fun testDefaultMessage() {
		val server = TeamscaleServer()
		server.partition = "Unit Test"
		server.revision = "rev123"

		val message = server.message
		val normalizedMessage = message!!.replace("uploaded at .*".toRegex(), "uploaded at DATE")
			.replace("hostname: .*".toRegex(), "hostname: HOST")
		Assertions.assertEquals(
			"Unit Test coverage uploaded at DATE\n\nuploaded from hostname: HOST\nfor revision: rev123",
			normalizedMessage
		)
	}
}