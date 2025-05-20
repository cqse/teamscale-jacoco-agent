plugins {
	com.teamscale.`kotlin-convention`
	com.teamscale.`system-test-convention`
}

tasks.test {
	teamscaleAgent(
		mapOf(
			"http-server-port" to "$agentPort",
			"tia-mode" to "teamscale-upload",
			"mode" to "testwise",
			"teamscale-server-url" to "http://localhost:$teamscalePort",
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