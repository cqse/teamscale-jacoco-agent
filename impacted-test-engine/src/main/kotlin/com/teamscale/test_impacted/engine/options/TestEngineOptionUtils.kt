package com.teamscale.test_impacted.engine.options

import com.teamscale.client.CommitDescriptor
import com.teamscale.client.StringUtils.isEmpty
import org.junit.platform.engine.ConfigurationParameters
import java.util.*
import java.util.function.Function

object TestEngineOptionUtils {

	private const val PREFIX = "teamscale.test.impacted."

	/** Returns the [TestEngineOptions] configured in the [Properties].  */
	fun getEngineOptions(configurationParameters: ConfigurationParameters): TestEngineOptions {
		val propertyReader = PrefixingPropertyReader(PREFIX, configurationParameters)
		val shouldRunImpactedTests = propertyReader.getBoolean("runImpacted", true)

		val serverOptions = if (shouldRunImpactedTests) {
			createServerOptions(propertyReader)
		} else null

		return TestEngineOptions.builder()
			.serverOptions(serverOptions)
			.partition(propertyReader.getString("partition"))
			.runImpacted(shouldRunImpactedTests)
			.runAllTests(propertyReader.getBoolean("runAllTests", false))
			.includeAddedTests(propertyReader.getBoolean("includeAddedTests", true))
			.includeFailedAndSkipped(propertyReader.getBoolean("includeFailedAndSkipped", true))
			.endCommit(propertyReader.getCommitDescriptor("endCommit"))
			.endRevision(propertyReader.getString("endRevision"))
			.baseline(propertyReader.getString("baseline"))
			.baselineRevision(propertyReader.getString("baselineRevision"))
			.repository(propertyReader.getString("repository"))
			.testCoverageAgentUrls(propertyReader.getStringList("agentsUrls"))
			.includedTestEngineIds(propertyReader.getStringList("includedEngines"))
			.excludedTestEngineIds(propertyReader.getStringList("excludedEngines"))
			.reportDirectory(propertyReader.getString("reportDirectory"))
			.build()
	}

	private fun createServerOptions(propertyReader: PrefixingPropertyReader) =
		ServerOptions.builder()
			.url(propertyReader.getString("server.url"))
			.project(propertyReader.getString("server.project"))
			.userName(propertyReader.getString("server.userName"))
			.userAccessToken(propertyReader.getString("server.userAccessToken"))
			.build()

	private class PrefixingPropertyReader(
		private val prefix: String,
		private val configurationParameters: ConfigurationParameters
	) {
		fun getString(propertyName: String): String =
			configurationParameters[prefix + propertyName].orElse("")

		fun getBoolean(propertyName: String, defaultValue: Boolean): Boolean =
			configurationParameters[prefix + propertyName].map { it.toBoolean() }.orElse(defaultValue)

		fun getCommitDescriptor(propertyName: String): CommitDescriptor? =
			configurationParameters[prefix + propertyName].map { CommitDescriptor.parse(it) }.orElse(null)

		fun getStringList(propertyName: String): List<String> =
			configurationParameters[prefix + propertyName]
				.map { it.split(",").filterNot(String::isEmpty) }
				.orElse(emptyList())
	}
}
