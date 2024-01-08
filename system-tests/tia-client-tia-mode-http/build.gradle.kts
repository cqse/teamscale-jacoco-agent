plugins {
	com.teamscale.`system-test-convention`
}

tasks.test {
	teamscaleAgent(
		mapOf(
			"http-server-port" to "$agentPort",
			"tia-mode" to "http",
			"mode" to "testwise",
			"includes" to "*systemundertest.*"
		)
	)
}

dependencies {
	implementation(project(":tia-client"))
}
