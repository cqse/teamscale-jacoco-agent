plugins {
	id("com.teamscale.java-convention")
	id("com.teamscale.agent-jar")
}

val provider = SystemTestPorts.registerWith(project)

tasks.test {
	dependsOn(":agent:shadowJar")
	usesService(provider)

	val teamscalePort = provider.get().pickFreePort()
	val agentPort = provider.get().pickFreePort()
	extensions.create<PortsExtension>("ports", provider).apply {
		this.teamscalePort = teamscalePort
		this.agentPort = agentPort
	}

	systemProperties("agentPort" to agentPort, "teamscalePort" to teamscalePort)
	environment("AGENT_VERSION", version)
	environment("AGENT_PATH", agentJar)
	environment("TEAMSCALE_PORT", teamscalePort)
	environment("AGENT_PORT", agentPort)

	val dir = layout.projectDirectory.dir(logFilePath)
	doFirst {
		dir.asFile.deleteRecursively()
	}
}

dependencies {
	testImplementation(project(":common-system-test"))
}
