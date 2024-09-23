plugins {
	com.teamscale.`system-test-convention`
}

tasks.test {
	environment("AGENT_VERSION", version)
	environment("TEAMSCALE_PORT", teamscalePort)
	// install dependencies needed by the Maven test project
	dependsOn(":publishToMavenLocal")
}
