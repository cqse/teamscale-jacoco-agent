plugins {
	com.teamscale.`system-test-convention`
}

tasks.test {
	teamscaleAgent(mapOf("debug" to "true"))

	doFirst {
		// the test checks if a log file is written, so we need to remove all leftover agent temp directories
		// to avoid false-positive test failures
		val leftoverAgentTempDirectories = file(System.getProperty("java.io.tmpdir")).listFiles()
			.filter { it.name.contains("teamscale-java-profiler") }
		leftoverAgentTempDirectories.forEach { it.deleteRecursively() }
	}
}
