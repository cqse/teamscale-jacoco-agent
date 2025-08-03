import java.io.Serializable

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

	val shadowJarOutputs = project.project(":agent").tasks["shadowJar"].outputs.files
	doFirst("copyAgent", CopyAgent(shadowJarOutputs, libDir, agentJar))
}

class CopyAgent(
	val shadowJarOutputs: FileCollection,
	val libDir: File,
	val agentJar: File
) : Action<Task>, Serializable {
	override fun execute(t: Task) {
		libDir.mkdir()
		shadowJarOutputs.singleFile.copyTo(agentJar, overwrite = true)
	}
}

tasks.withType<JavaExec> {
	createAgentCopy()
}

tasks.test {
	createAgentCopy()
}

