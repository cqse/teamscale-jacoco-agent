package eu.cqse.config

import eu.cqse.teamscale.report.util.ClasspathWildcardIncludeFilter
import okhttp3.HttpUrl
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

    private var remoteUrl: HttpUrl? = null

    var localPort: Int = 8000
        private set

    fun isLocalAgent(): Boolean {
        return remoteUrl == null
    }

    val url
        get() = remoteUrl ?: "http://127.0.0.1:$localPort/"

    fun useRemoteAgent(url: String) {
        this.remoteUrl = HttpUrl.parse(url)
    }

    fun getDumpDirectory(project: Project): File {
        return dumpDirectory ?: project.file("${project.buildDir}/classesDump")
    }

    fun copyWithDefault(toCopy: AgentConfiguration, default: AgentConfiguration) {
        executionData = toCopy.executionData ?: default.executionData
        dumpClasses = toCopy.dumpClasses ?: default.dumpClasses ?: false
        dumpDirectory = toCopy.dumpDirectory ?: default.dumpDirectory
        includes = toCopy.includes ?: default.includes
        excludes = toCopy.excludes ?: default.excludes
    }

    fun getFilter(): Predicate<String>? {
        return ClasspathWildcardIncludeFilter(
            includes?.joinToString(":") { "*$it".replace('/', '.') },
            excludes?.joinToString(":") { "*$it".replace('/', '.') }
        )
    }
}
