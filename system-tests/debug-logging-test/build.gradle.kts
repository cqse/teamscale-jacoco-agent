plugins {
	id("com.teamscale.system-test-convention")
}

tasks.test {
	val logFilePath = "logTest"
	teamscaleAgent(mapOf("debug" to "$logFilePath"))
}
