plugins {
	id("com.teamscale.java-convention")
}

SystemTestPorts.registerWith(project)

tasks.test {
	dependsOn(":agent:shadowJar")
	usesService(SystemTestPorts.provider)
	systemProperties("agentPort" to agentPort, "teamscalePort" to teamscalePort)
}

dependencies {
	testImplementation(project(":common-system-test"))
}
