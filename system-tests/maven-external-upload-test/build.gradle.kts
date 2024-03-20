plugins {
	com.teamscale.`system-test-convention`
}

tasks.test {
	environment("AGENT_VERSION", rootProject.extra["appVersion"].toString())
	environment("TEAMSCALE_PORT", teamscalePort)
	// install dependencies needed by the Maven test project
	dependsOn(":publishToMavenLocal")
}
