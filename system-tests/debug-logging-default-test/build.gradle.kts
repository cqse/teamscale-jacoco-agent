plugins {
	com.teamscale.`kotlin-convention`
	com.teamscale.`system-test-convention`
}

tasks.test {
	teamscaleAgent(mapOf("debug" to "true"))
	// make sure this test doesn't use the system temp dir since it relies on only this test writing there
	systemProperty("java.io.tmpdir", temporaryDir)
}
