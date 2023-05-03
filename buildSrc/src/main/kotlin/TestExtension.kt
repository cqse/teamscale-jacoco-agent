import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.get
import java.io.File

/** Determines the file under which the shadowJar task will store the final Teamscale JaCoCo Agent jar. */
val Test.agentJar: File
    get() {
        return project.project(":agent").tasks["shadowJar"].outputs.files.singleFile
    }

/** Adds a convenient way to attach the Teamscale JaCoCo agent to the test JVM with the given options in a readable map format. */
fun Test.teamscaleAgent(options: Map<String, String>) {
    jvmArgs(
        "-javaagent:$agentJar=${options.entries.joinToString(separator = ",") { "${it.key}=${it.value}" }}"
    )
}
