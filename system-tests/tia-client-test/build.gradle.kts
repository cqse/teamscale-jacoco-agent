plugins {
	id("com.teamscale.system-test-convention")
}

tasks.test {
	/** These ports must match what is configured in the SystemTest class. */
	val fakeTeamscalePort = 65432
	val agentPort = 65433
	teamscaleAgent(
		mapOf(
			"http-server-port" to "$agentPort",
			"tia-mode" to "teamscale-upload",
			"mode" to "testwise",
			"teamscale-server-url" to "http://localhost:$fakeTeamscalePort",
			"teamscale-user" to "fake",
			"teamscale-access-token" to "fake",
			"teamscale-project" to "p",
			"teamscale-partition" to "part",
			"teamscale-commit" to "master:12345",
			"includes" to "*systemundertest.*"
		)
	)
}

dependencies {
	implementation(project(":tia-client"))
}