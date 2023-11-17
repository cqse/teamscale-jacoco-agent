import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import java.io.File

/** Determines the file under which the shadowJar task will store the final Teamscale JaCoCo Agent jar. */
val Task.agentJar: File
	get() {
		return extra["agentJar"] as? File
			?: throw GradleException("You must apply the com.teamscale.agent-jar plugin to use this property")
	}

/** Adds a convenient way to attach the Teamscale JaCoCo agent to the JVM with the given options in a readable map format. */
fun JavaExec.teamscaleAgent(options: Map<String, String>) {
	jvmArgs(
		"-javaagent:$agentJar=${options.entries.joinToString(separator = ",") { "${it.key}=${it.value}" }}"
	)
}

/** Adds a convenient way to attach the Teamscale JaCoCo agent to the test JVM with the given options in a readable map format. */
fun Test.teamscaleAgent(options: Map<String, String>) {
	jvmArgs(
		"-javaagent:$agentJar=${options.entries.joinToString(separator = ",") { "${it.key}=${it.value}" }}"
	)
}
