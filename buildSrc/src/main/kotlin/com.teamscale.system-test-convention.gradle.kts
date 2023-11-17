plugins {
	id("com.teamscale.java-convention")
	id("com.teamscale.agent-jar")
}

val provider = SystemTestPorts.registerWith(project)

tasks.test {
	dependsOn(":agent:shadowJar")
	usesService(provider)
	portProvider = provider
	teamscalePort = provider.get().pickFreePort()
	agentPort = provider.get().pickFreePort()
	systemProperties("agentPort" to agentPort, "teamscalePort" to teamscalePort)
}

dependencies {
	testImplementation(project(":common-system-test"))
}
