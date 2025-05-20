plugins {
	com.teamscale.`kotlin-convention`
	com.teamscale.`system-test-convention`
}

tasks.test {
	// install dependencies needed by the Maven test project
	dependsOn(":publishToMavenLocal")
}
