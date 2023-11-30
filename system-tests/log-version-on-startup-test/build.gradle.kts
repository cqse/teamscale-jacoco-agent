plugins {
	com.teamscale.`system-test-convention`
}

tasks.test {
	val logFilePath = "logTest"
	environment("AGENT_VERSION", version)
	teamscaleAgent(mapOf("debug" to logFilePath))
}
