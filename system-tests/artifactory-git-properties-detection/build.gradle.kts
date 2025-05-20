plugins {
	com.teamscale.`kotlin-convention`
	com.teamscale.`system-test-convention`
}

sourceSets {
	main {
		output.setResourcesDir(java.classesDirectory.get().asFile)
	}
}

tasks.test {
	val artifactoryPort = portProvider.get().pickFreePort()
	systemProperty("artifactoryPort", artifactoryPort)

	teamscaleAgent(
		mapOf(
			"http-server-port" to "$agentPort",
			"artifactory-url" to "http://localhost:$artifactoryPort",
			"artifactory-api-key" to "fake",
			"artifactory-partition" to "some-test"
		)
	)
}
