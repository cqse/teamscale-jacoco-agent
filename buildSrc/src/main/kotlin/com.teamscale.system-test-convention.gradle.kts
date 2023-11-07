plugins {
	id("com.teamscale.java-convention")
}

tasks.test {
	dependsOn(":agent:shadowJar")
	systemProperties("agentPort" to agentPort, "teamscalePort" to teamscalePort)
}

dependencies {
	testImplementation(project(":common-system-test"))
}
