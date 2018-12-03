package com.teamscale.config

import com.teamscale.ArgumentAppender
import com.teamscale.TeamscalePlugin
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import okhttp3.HttpUrl
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.Test
import java.io.File
import java.io.Serializable
import java.util.function.Predicate

/**
 * Configuration for the the Teamscale JaCoCo agent.
 * The agent can either be configured to run locally or to connect to an
 * already running remote testwise coverage server.
 */
class AgentConfiguration : Serializable {

    /**
     * Whether instrumented class files should be dumped to disk before instrumentation.
     * This option needs to be set to true if other bytecode manipulation happens during
     * runtime e.g. by another profiler to get coverage information.
     */
    var dumpClasses: Boolean? = null

    /**
     * The directory into which class files should be dumped (see #dumpClasses).
     */
    var dumpDirectory: File? = null

    /**
     * List of include patterns (wildcard ? / * / **) of classes that should be instrumented.
     * Will be matched against the fully qualified class name of the classes.
     */
    var includes: List<String>? = null

    /**
     * List of exclude patterns (wildcard ? / * / **) of classes that should not be instrumented.
     * Default is "org.junit.**"
     * Will be matched against the fully qualified class name of the classes.
     */
    var excludes: List<String>? = null

    /** The directory to store test artifacts into. */
    var testArtifactDestination: File? = null

    /** The port at which the local testwise coverage server should be started. */
    var localPort: Int? = null

    /** List of remote agents to connect to. */
    var agents: MutableList<TeamscaleAgent> = mutableListOf()

    /**
     * Returns whether an agent should be attached to the same JVM as the test runner.
     */
    var useLocalAgent: Boolean? = null

    val localAgent: TeamscaleAgent by lazy {
        TeamscaleAgent(HttpUrl.parse("http://127.0.0.1:$localPort/"))
    }

    /** Returns the directory into which class files should be dumped when #dumpClasses is enabled. */
    fun getAllAgents(): List<TeamscaleAgent> {
        val allAgents = ArrayList(agents)
        if (useLocalAgent != false) {
            allAgents.add(localAgent)
        }
        return allAgents
    }

    /**
     * Configures the Teamscale plugin to use a remote agent instead of a local one.
     * @param url The url (including the port) of the http server
     *            started by the remote agent in testwise coverage mode.
     */
    @JvmOverloads
    fun useRemoteAgent(url: String = "http://127.0.0.1:8124/") {
        agents.add(TeamscaleAgent(HttpUrl.parse(url)))
    }

    /** Returns the directory into which class files should be dumped when #dumpClasses is enabled. */
    fun getDumpDirectory(project: Project): File {
        return dumpDirectory ?: project.file("${project.buildDir}/classesDump")
    }

    /** Returns the directory into which test artifacts should be written to. */
    fun getTestArtifactDestination(project: Project, taskName: String): File {
        return testArtifactDestination ?: project.file(
            "${project.buildDir}/tmp/jacoco/${project.name}-$taskName"
        )
    }

    /** Creates a copy of the agent configuration, setting all non-set values to their default value. */
    fun copyWithDefault(toCopy: AgentConfiguration, default: AgentConfiguration) {
        testArtifactDestination = toCopy.testArtifactDestination ?: default.testArtifactDestination
        dumpClasses = toCopy.dumpClasses ?: default.dumpClasses ?: false
        dumpDirectory = toCopy.dumpDirectory ?: default.dumpDirectory
        includes = toCopy.includes ?: default.includes
        excludes = toCopy.excludes ?: default.excludes ?: listOf("org.junit.**")
        useLocalAgent = toCopy.useLocalAgent ?: default.useLocalAgent ?: true
        localPort = toCopy.localPort ?: default.localPort ?: 8123
        agents = mutableListOf(toCopy.agents, default.agents).flatten().toMutableList()
    }

    fun getClassFileDirs(project: Project, testTask: Test): Set<File> {
        return if (dumpClasses == true) {
            project.files(getDumpDirectory(project)).files
        } else {
            testTask.classpath.files
        }
    }

    /** Returns a filter predicate that respects the configured include and exclude patterns. */
    fun getFilter(): SerializableFilter {
        return SerializableFilter(includes, excludes)
    }

    inner class TeamscaleAgent(val url: HttpUrl) {

        /** Builds the jvm argument to start the impacted test executor. */
        fun getJvmArgs(project: Project, taskName: String): String {
            val builder = StringBuilder()
            val argument = ArgumentAppender(builder)
            builder.append("-javaagent:")
            val agentJar = project.configurations.getByName(TeamscalePlugin.teamscaleJaCoCoAgentConfiguration)
                .filter { it.name.startsWith("teamscale-jacoco-agent") }.first()
            builder.append(agentJar.canonicalPath)
            builder.append("=")

            appendArguments(argument, project, taskName)

            return builder.toString()
        }

        /**
         * Appends the configuration for starting a local instance of the testwise coverage server to the
         * java agent arguments.
         */
        private fun appendArguments(
            argument: ArgumentAppender,
            project: Project,
            taskName: String
        ) {
            argument.append("out", getTestArtifactDestination(project, taskName))
            argument.append("includes", includes)
            argument.append("excludes", excludes)

            if (dumpClasses == true) {
                argument.append("classdumpdir", getDumpDirectory(project))
            }

            argument.append("http-server-port", url.port())
        }
    }
}


class SerializableFilter(private val includes: List<String>?, private val excludes: List<String>?) : Serializable {

    /** Returns a filter predicate that respects the configured include and exclude patterns. */
    fun getPredicate(): Predicate<String>? {
        return ClasspathWildcardIncludeFilter(
            includes?.joinToString(":") { "*$it".replace('/', '.') },
            excludes?.joinToString(":") { "*$it".replace('/', '.') }
        )
    }

}