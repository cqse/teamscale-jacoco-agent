plugins {
	com.teamscale.`system-test-convention`
}

tasks.test {
	val logFilePath = "logTest"
	environment("AGENT_VERSION", rootProject.extra["appVersion"].toString())
	teamscaleAgent(mapOf("debug" to logFilePath))
}
