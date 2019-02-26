package com.teamscale.config

import com.teamscale.ArgumentAppender
import com.teamscale.TeamscalePlugin
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import okhttp3.HttpUrl
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.io.File
import java.io.Serializable
import java.util.function.Predicate


/**
 * Configuration for the the Teamscale JaCoCo agent.
 * The agent can either be configured to run locally or to connect to an
 * already running remote testwise coverage server.
 */
class AgentConfiguration(val project: Project, val jacocoExtension: JacocoTaskExtension) : Serializable {

    /** The property object backing #destination. */
    private var destinationProperty: Property<File> = project.objects.property(File::class.java)

    /** The destination directory to store test artifacts into. */
    var destination: File
        get() = destinationProperty.get()
        set(value) {
            destinationProperty.set(value)
        }

    fun setDestination(provider: Provider<File>) {
        destinationProperty.set(provider)
    }

    /** The local agent's server url to connect to. */
    var localAgent: TeamscaleAgent? = TeamscaleAgent(HttpUrl.parse("http://127.0.0.1:8123/"))

    /** A remote agent's server url to connect to. */
    var remoteAgent: TeamscaleAgent? = null

    /** Returns the directory into which class files should be dumped when #dumpClasses is enabled. */
    fun getAllAgents(): List<TeamscaleAgent> {
        val allAgents = mutableListOf<TeamscaleAgent>()
        localAgent?.let { allAgents.add(it) }
        remoteAgent?.let { allAgents.add(it) }
        return allAgents
    }

    /**
     * Configures the Teamscale plugin to use a local agent.
     * @param url The url (including the port) of the http server
     *            that should be started in testwise coverage mode for the local java process.
     */
    @JvmOverloads
    fun useLocalAgent(url: String = "http://127.0.0.1:8123/") {
        localAgent = TeamscaleAgent(HttpUrl.parse(url))
    }

    /**
     * Configures the Teamscale plugin to use a remote agent additional to the local one.
     * @param url The url (including the port) of the http server
     *            started by the remote agent in testwise coverage mode.
     */
    @JvmOverloads
    fun useRemoteAgent(url: String = "http://127.0.0.1:8124/") {
        remoteAgent = TeamscaleAgent(HttpUrl.parse(url))
    }

    /** Returns a filter predicate that respects the configured include and exclude patterns. */
    fun getFilter(): SerializableFilter {
        return SerializableFilter(jacocoExtension.includes, jacocoExtension.excludes)
    }

    inner class TeamscaleAgent(val url: HttpUrl) {

        /** Builds the jvm argument to start the impacted test executor. */
        fun getJvmArgs(
        ): String {
            val builder = StringBuilder()
            val argument = ArgumentAppender(builder)
            builder.append("-javaagent:")
            val agentJar = project.configurations.getByName(TeamscalePlugin.teamscaleJaCoCoAgentConfiguration)
                .filter { it.name.startsWith("teamscale-jacoco-agent") }.first()
            builder.append(agentJar.canonicalPath)
            builder.append("=")

            appendArguments(argument, jacocoExtension)

            return builder.toString()
        }

        /**
         * Appends the configuration for starting a local instance of the testwise coverage server to the
         * java agent arguments.
         */
        private fun appendArguments(
            argument: ArgumentAppender,
            jacocoExtension: JacocoTaskExtension
        ) {
            argument.append("out", destination)
            argument.append("includes", jacocoExtension.includes)
            argument.append("excludes", jacocoExtension.excludes)
            argument.append("http-server-port", url.port())
        }
    }
}


class SerializableFilter(private val includes: List<String>?, private val excludes: List<String>?) : Serializable {

    /** Returns a filter predicate that respects the configured wildcard include and exclude patterns. */
    fun getPredicate(): Predicate<String>? {
        return ClasspathWildcardIncludeFilter(
            includes?.joinToString(":") { "*$it".replace('/', '.') },
            excludes?.joinToString(":") { "*$it".replace('/', '.') }
        )
    }

}