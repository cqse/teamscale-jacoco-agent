plugins {
	id("com.teamscale.system-test-convention")
}

tasks.test {
	/** These ports must match what is configured in the SystemTest class. */
	val fakeRedirectPort = 65440
	val agentPort = 65439
	teamscaleAgent(
		mapOf(
			"http-server-port" to "$agentPort",
			"teamscale-server-url" to "http://localhost:$fakeRedirectPort",
			"teamscale-user" to "fake",
			"teamscale-access-token" to "fake",
			"teamscale-project" to "p",
			"teamscale-partition" to "part",
			"teamscale-commit" to "master:12345",
			"includes" to "**"
		)
	)
}
