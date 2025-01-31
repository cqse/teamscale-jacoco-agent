package com.teamscale.maven.tia

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

/**
 * Composes a new argLine based on the current one and input about the desired agent configuration.
 */
class ArgLine(
	private val additionalAgentOptions: Array<String>?,
	private val agentLogLevel: String,
	private val agentJarFile: Path,
	private val agentConfigFile: Path,
	private val logFilePath: Path
) {
	/**
	 * Takes the given old argLine, removes any previous invocation of our agent and prepends a new one based on the
	 * constructor parameters of this class. Preserves all other options in the old argLine.
	 */
	fun prependTo(oldArgLine: String?): String {
		val jvmOptions = createJvmOptions()
		if (oldArgLine.isNullOrBlank()) {
			return jvmOptions
		}

		return "$jvmOptions $oldArgLine"
	}

	private fun createJvmOptions() =
		listOf(
			"-Dteamscale.markstart",
			createJavaAgentArgument(),
			"-DTEAMSCALE_AGENT_LOG_FILE=$logFilePath",
			"-DTEAMSCALE_AGENT_LOG_LEVEL=$agentLogLevel",
			"-Dteamscale.markend"
		).joinToString(" ") { quoteCommandLineOptionIfNecessary(it) }

	private fun createJavaAgentArgument(): String {
		val agentOptions = mutableListOf<String>().apply {
			add("config-file=" + agentConfigFile.toAbsolutePath())
			addAll(listOf(*ArrayUtils.nullToEmpty(additionalAgentOptions)))
		}
		return "-javaagent:${agentJarFile.toAbsolutePath()}=${agentOptions.joinToString(",")}"
	}

	companion object {
		/** Applies the given [ArgLine] to the given [MavenSession].  */
		fun applyToMavenProject(
			argLine: ArgLine,
			session: MavenSession,
			log: Log,
			userDefinedPropertyName: String,
			isIntegrationTest: Boolean
		) {
			val mavenProject = session.currentProject
			val effectiveProperty = getEffectiveProperty(
				userDefinedPropertyName, mavenProject, isIntegrationTest
			)

			val oldArgLine = effectiveProperty.getValue(session)
			val newArgLine = argLine.prependTo(oldArgLine)

			effectiveProperty.setValue(session, newArgLine)
			log.info("${effectiveProperty.propertyName} set to $newArgLine")
		}

		/**
		 * Removes any occurrences of our agent from all [ArgLineProperty.STANDARD_PROPERTIES].
		 */
		fun cleanOldArgLines(session: MavenSession, log: Log) {
			ArgLineProperty.STANDARD_PROPERTIES.forEach { property ->
				val oldArgLine = property.getValue(session)
				if (oldArgLine.isBlank()) return@forEach

				val newArgLine = removePreviousTiaAgent(oldArgLine)
				if (oldArgLine != newArgLine) {
					log.info("Removed agent from property ${property.propertyName}")
					property.setValue(session, newArgLine)
				}
			}
		}

		private fun quoteCommandLineOptionIfNecessary(option: String) =
			if (StringUtils.containsWhitespace(option)) "'$option'" else option

		/**
		 * Determines the property in which to set the argLine. By default, this is the property used by the testing
		 * framework of the current project's packaging. The user may override this by providing their own property name.
		 */
		private fun getEffectiveProperty(
			userDefinedPropertyName: String,
			mavenProject: MavenProject,
			isIntegrationTest: Boolean
		): ArgLineProperty {
			if (userDefinedPropertyName.isNotBlank()) {
				return ArgLineProperty.projectProperty(userDefinedPropertyName)
			}

			if (isIntegrationTest && hasSpringBootPluginEnabled(mavenProject)) {
				return ArgLineProperty.SPRING_BOOT_ARG_LINE
			}

			if ("eclipse-test-plugin" == mavenProject.packaging) {
				return ArgLineProperty.TYCHO_ARG_LINE
			}
			return ArgLineProperty.SUREFIRE_ARG_LINE
		}

		private fun hasSpringBootPluginEnabled(mavenProject: MavenProject) =
			mavenProject.buildPlugins.any { it.artifactId == "spring-boot-maven-plugin" }

		/**
		 * Removes any previous invocation of our agent from the given argLine. This is necessary in case we want to
		 * instrument unit and integration tests but with different arguments.
		 */
		@JvmStatic
		fun removePreviousTiaAgent(argLine: String?) =
			argLine?.replace("-Dteamscale.markstart.*teamscale.markend".toRegex(), "") ?: ""
	}
}
