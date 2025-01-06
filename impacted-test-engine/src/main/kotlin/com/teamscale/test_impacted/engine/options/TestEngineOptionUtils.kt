package com.teamscale.test_impacted.engine.options

import com.teamscale.client.CommitDescriptor
import org.junit.platform.engine.ConfigurationParameters
import java.util.*

object TestEngineOptionUtils {
	/** Represents the constant prefix used for property keys in the configuration parameters for the test engine. */
	private const val PREFIX = "teamscale.test.impacted."

	/** Returns the [TestEngineOptions] configured in the [Properties].  */
	fun getEngineOptions(configurationParameters: ConfigurationParameters): TestEngineOptions {
		val propertyReader = PrefixingPropertyReader(PREFIX, configurationParameters)
		val shouldRunImpactedTests = propertyReader.getBoolean("runImpacted", true)

		val serverOptions = if (shouldRunImpactedTests) {
			propertyReader.createServerOptions()
		} else null

		return TestEngineOptions(
			serverOptions = serverOptions,
			partition = propertyReader.getString("partition"),
			runImpacted = shouldRunImpactedTests,
			runAllTests = propertyReader.getBoolean("runAllTests", false),
			includeAddedTests = propertyReader.getBoolean("includeAddedTests", true),
			includeFailedAndSkipped = propertyReader.getBoolean("includeFailedAndSkipped", true),
			endCommit = propertyReader.getCommitDescriptor("endCommit"),
			endRevision = propertyReader.getString("endRevision"),
			baseline = propertyReader.getString("baseline"),
			baselineRevision = propertyReader.getString("baselineRevision"),
			repository = propertyReader.getString("repository"),
			testCoverageAgentUrls = propertyReader.getStringList("agentsUrls"),
			includedTestEngineIds = propertyReader.getStringList("includedEngines").toSet(),
			excludedTestEngineIds = propertyReader.getStringList("excludedEngines").toSet(),
			reportDirectoryPath = propertyReader.getString("reportDirectory")
		)
	}

	private fun PrefixingPropertyReader.createServerOptions() =
		ServerOptions(
			getString("server.url")!!,
			getString("server.project")!!,
			getString("server.userName")!!,
			getString("server.userAccessToken")!!
		)

	private class PrefixingPropertyReader(
		private val prefix: String,
		private val configurationParameters: ConfigurationParameters
	) {
		/**
		 * Retrieves the string value associated with a given property name.
		 *
		 * @param propertyName The name of the property to retrieve.
		 * @return The string value of the property if it exists, or null if not found.
		 */
		fun getString(propertyName: String): String? =
			configurationParameters[prefix + propertyName].orElse(null)

		/**
		 * Retrieves the boolean value associated with a specified property name.
		 *
		 * @param propertyName The name of the property for which the boolean value should be retrieved.
		 * @param defaultValue The default value to return if the property is not found or cannot be converted to a boolean.
		 * @return The boolean value of the property if it exists and can be converted, otherwise the specified default value.
		 */
		fun getBoolean(propertyName: String, defaultValue: Boolean): Boolean =
			configurationParameters[prefix + propertyName].map { it.toBoolean() }.orElse(defaultValue)

		/**
		 * Retrieves a [CommitDescriptor] associated with the specified property name from the configuration parameters.
		 * The property value is parsed into a [CommitDescriptor] object if present.
		 *
		 * @param propertyName The name of the property to retrieve and parse.
		 * @return The parsed `CommitDescriptor` if the property exists and is valid, otherwise null.
		 */
		fun getCommitDescriptor(propertyName: String): CommitDescriptor? =
			configurationParameters[prefix + propertyName].map { CommitDescriptor.parse(it) }.orElse(null)

		/**
		 * Retrieves a list of strings associated with the specified property name.
		 *
		 * @param propertyName The name of the property for which the string list should be retrieved.
		 * @return A list of non-blank, trimmed strings derived from the property value.
		 *         If the property does not exist, an empty list is returned.
		 */
		fun getStringList(propertyName: String): List<String> =
			configurationParameters[prefix + propertyName]
				.map { it.split(",").map(String::trim).filterNot(String::isBlank) }
				.orElse(emptyList())
	}
}
