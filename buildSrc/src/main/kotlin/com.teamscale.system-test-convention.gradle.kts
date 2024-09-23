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
	environment("AGENT_VERSION", version)
	environment("AGENT_PATH", agentJar)
	environment("TEAMSCALE_PORT", teamscalePort)
	environment("AGENT_PORT", agentPort)
}

dependencies {
	testImplementation(project(":common-system-test"))
}
