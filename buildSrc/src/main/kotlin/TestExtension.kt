import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.get
import java.io.File

val Test.agentJar: File
    get() {
        return project.project(":agent").tasks["shadowJar"].outputs.files.files.first()
    }

fun Test.teamscaleAgent(options: Map<String, String>) {
    jvmArgs(
        "-javaagent:$agentJar=${options.entries.joinToString(separator = ",") { "${it.key}=${it.value}" }}"
    )
}
