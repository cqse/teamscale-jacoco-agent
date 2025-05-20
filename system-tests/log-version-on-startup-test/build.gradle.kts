plugins {
	com.teamscale.`kotlin-convention`
	com.teamscale.`system-test-convention`
}

tasks.test {
	val logFilePath = "logTest"
	teamscaleAgent(mapOf("debug" to logFilePath))
}
