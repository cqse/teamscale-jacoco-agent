package com.teamscale.config

import com.teamscale.ArgumentAppender
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import okhttp3.HttpUrl
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.io.Serializable
import javax.inject.Inject


/**
 * Configuration for the Teamscale JaCoCo agent.
 * The agent can either be configured to run locally or to connect to an
 * already running remote testwise coverage server.
 */
@Suppress("unused")
abstract class AgentConfiguration @Inject constructor(
	@InputFiles val teamscaleJacocoAgentConfiguration: FileCollection,
	@Nested val jacocoExtension: JacocoTaskExtension
) : Serializable {

	/** The property object backing #destination. */
	@get:OutputDirectory
	abstract val destination: DirectoryProperty

	/** The local agent's server url to connect to. */
	@get:Internal
	abstract val localAgent: Property<TeamscaleAgent>

	/** A remote agent's server url to connect to. */
	@get:Internal
	abstract val remoteAgent: Property<TeamscaleAgent>

	/** Returns the directory into which class files should be dumped when #dumpClasses is enabled. */
	@Input
	fun getAllAgentUrls(): List<String> {
		val allAgents = mutableListOf<TeamscaleAgent>()
		localAgent.orNull?.let { allAgents.add(it) }
		remoteAgent.orNull?.let { allAgents.add(it) }
		return allAgents.map { it.url.toString() }
	}

	/**
	 * Configures the Teamscale plugin to use a local agent.
	 * @param url The url (including the port) of the http server
	 *            that should be started in testwise coverage mode for the local java process.
	 */
	@JvmOverloads
	fun useLocalAgent(url: String = "http://127.0.0.1:8123/") {
		localAgent.set(TeamscaleAgent(HttpUrl.parse(url)!!))
	}

	/**
	 * Configures the Teamscale plugin to use a remote agent additional to the local one.
	 * @param url The url (including the port) of the http server
	 *            started by the remote agent in testwise coverage mode.
	 */
	@JvmOverloads
	fun useRemoteAgent(url: String = "http://127.0.0.1:8124/") {
		remoteAgent.set(TeamscaleAgent(HttpUrl.parse(url)!!))
	}

	/** Returns a filter predicate that respects the configured wildcard include and exclude patterns. */
	@Internal
	fun getPredicate(): ClasspathWildcardIncludeFilter {
		return ClasspathWildcardIncludeFilter(
			jacocoExtension.includes?.joinToString(":") { "*$it".replace('/', '.') },
			jacocoExtension.excludes?.joinToString(":") { "*$it".replace('/', '.') }
		)
	}

	inner class TeamscaleAgent(val url: HttpUrl) {

		/** Builds the jvm argument to start the impacted test executor. */
		fun getJvmArgs(
		): String {
			val builder = StringBuilder()
			val argument = ArgumentAppender(builder)
			builder.append("-javaagent:")
			val agentJar = teamscaleJacocoAgentConfiguration.singleFile
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
			argument.append("out", destination.asFile.get())
			argument.append("includes", jacocoExtension.includes)
			argument.append("excludes", jacocoExtension.excludes)
			argument.append("mode", "testwise")
			argument.append("http-server-port", url.port())
		}
	}
}
