import java.io.Serializable

plugins {
	id("com.teamscale.java-convention")
}

/**
 * Creates a copy of the agent jar file in the temporary directory of this task
 * to isolate it from other tasks running in parallel.
 */
fun Task.createAgentCopy() {
	val shadowJarOutputs = project.project(":agent").tasks.named("shadowJar").map { it.outputs.files.singleFile }
	dependsOn(shadowJarOutputs)
	doFirst("copyAgent", CopyAgent(shadowJarOutputs, agentJar))
}

class CopyAgent(
	val shadowJarOutputs: Provider<File>,
	val agentJar: File
) : Action<Task>, Serializable {
	override fun execute(t: Task) {
		agentJar.parentFile.mkdir()
		shadowJarOutputs.get().copyTo(agentJar, overwrite = true)
	}
}

tasks.withType<JavaExec> {
	createAgentCopy()
}

tasks.test {
	createAgentCopy()
}

