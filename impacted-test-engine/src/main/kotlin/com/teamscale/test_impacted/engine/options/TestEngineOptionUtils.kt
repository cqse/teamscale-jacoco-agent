package com.teamscale.test_impacted.engine.options

import com.teamscale.client.CommitDescriptor
import org.junit.platform.engine.ConfigurationParameters
import java.util.*

object TestEngineOptionUtils {

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
		fun getString(propertyName: String): String? =
			configurationParameters[prefix + propertyName].orElse(null)

		fun getBoolean(propertyName: String, defaultValue: Boolean): Boolean =
			configurationParameters[prefix + propertyName].map { it.toBoolean() }.orElse(defaultValue)

		fun getCommitDescriptor(propertyName: String): CommitDescriptor? =
			configurationParameters[prefix + propertyName].map { CommitDescriptor.parse(it) }.orElse(null)

		fun getStringList(propertyName: String): List<String> =
			configurationParameters[prefix + propertyName]
				.map { it.split(",").map(String::trim).filterNot(String::isBlank) }
				.orElse(emptyList())
	}
}
