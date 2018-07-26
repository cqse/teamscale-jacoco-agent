package eu.cqse.config

import eu.cqse.teamscale.report.util.AntPatternIncludeFilter
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.io.Serializable
import java.util.function.Predicate

class AgentConfiguration : Serializable {

    var executionData: File? = null
    var dumpClasses: Boolean? = null
    var dumpDirectory: File? = null
    var includes: List<String>? = null
    var excludes: List<String>? = null

    fun getExecutionData(project: Project, gradleTestTask: Task): File {
        return executionData
                ?: project.file("${project.buildDir}/jacoco/${project.name}-${gradleTestTask.name}.exec")
    }

    fun getDumpDirectory(project: Project): File {
        return dumpDirectory ?: project.file("${project.buildDir}/classesDump")
    }

    fun copyWithDefault(toCopy: AgentConfiguration, default: AgentConfiguration) {
        executionData = toCopy.executionData ?: default.executionData
        dumpClasses = toCopy.dumpClasses ?: default.dumpClasses ?: false
        dumpDirectory = toCopy.dumpDirectory ?: default.dumpDirectory
        includes = toCopy.includes ?: default.includes ?: emptyList()
        excludes = toCopy.excludes ?: default.excludes ?: emptyList()
    }

    fun getFilter(): Predicate<String>? {
        return AntPatternIncludeFilter(includes, excludes)
    }
}
