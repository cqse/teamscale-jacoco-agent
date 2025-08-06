plugins {
	com.teamscale.`kotlin-convention`
	com.teamscale.`system-test-convention`
}

tasks.test {
	teamscaleAgent(
		mapOf(
			"debug" to logFilePath
		)
	)
	teamscaleAgent(
		mapOf(
			"debug" to logFilePath
		)
	)
}

