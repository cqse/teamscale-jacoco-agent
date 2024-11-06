plugins {
	com.teamscale.`system-test-convention`
}

tasks.test {
	// install dependencies needed by the Maven test project
	dependsOn(":publishToMavenLocal")
}
