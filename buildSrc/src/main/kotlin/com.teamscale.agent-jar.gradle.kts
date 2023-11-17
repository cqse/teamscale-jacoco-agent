plugins {
	id("com.teamscale.java-convention")
}

/**
 * Creates a copy of the agent jar file in the temporary directory of this task
 * to isolate it from other tasks running in parallel.
 */
fun Task.createAgentCopy() {
	dependsOn(":agent:shadowJar")

	val libDir = temporaryDir.resolve("libs")
	val agentJar = libDir.resolve("agent.jar")
	extra["agentJar"] = agentJar

	doFirst {
		mkdir(libDir)
		copy {
			from(project.project(":agent").tasks["shadowJar"].outputs.files.singleFile)
			into(libDir)
			rename { _ -> agentJar.name }
		}
	}
}

tasks.withType<JavaExec>() {
	createAgentCopy()
}

tasks.test {
	createAgentCopy()
}

