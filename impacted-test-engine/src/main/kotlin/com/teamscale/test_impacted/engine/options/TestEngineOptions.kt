package com.teamscale.test_impacted.engine.options

import com.teamscale.client.CommitDescriptor
import com.teamscale.client.TeamscaleClient
import com.teamscale.test_impacted.engine.ImpactedTestEngineConfiguration
import com.teamscale.test_impacted.engine.TestDataWriter
import com.teamscale.test_impacted.engine.TestEngineRegistry
import com.teamscale.test_impacted.engine.executor.*
import com.teamscale.tia.client.ITestwiseCoverageAgentApi
import okhttp3.HttpUrl
import java.io.File
import kotlin.io.path.createDirectories

/** Represents options for the [com.teamscale.test_impacted.engine.ImpactedTestEngine].  */
class TestEngineOptions {

	companion object {
		private const val DEFAULT_RUN_IMPACTED = true
		private const val DEFAULT_INCLUDE_ADDED_TESTS = true
		private const val DEFAULT_INCLUDE_FAILED_AND_SKIPPED = true

		/** Returns the builder for [TestEngineOptions]. */
		@JvmStatic
		fun builder() = Builder()
	}

	private var serverOptions: ServerOptions? = null
	var partition: String? = null
	private var repository: String? = null

	private var runAllTests = false
	private var runImpacted = DEFAULT_RUN_IMPACTED
	private var includeAddedTests = DEFAULT_INCLUDE_ADDED_TESTS
	private var includeFailedAndSkipped = DEFAULT_INCLUDE_FAILED_AND_SKIPPED

	private var baseline: String? = null
	private var baselineRevision: String? = null
	private var endCommit: CommitDescriptor? = null
	private var endRevision: String? = null

	private var testwiseCoverageAgentApis = emptyList<ITestwiseCoverageAgentApi>()
	private var includedTestEngineIds = emptySet<String>()
	private var excludedTestEngineIds = emptySet<String>()

	private var reportDirectory: File? = null

	/** Creates the test engine configuration */
	fun createTestEngineConfiguration(): ImpactedTestEngineConfiguration {
		val testSorter = createTestSorter()
		val teamscaleAgentNotifier = createTeamscaleAgentNotifier()
		val testEngineRegistry = TestEngineRegistry(includedTestEngineIds, excludedTestEngineIds)
		val testDataWriter = TestDataWriter(reportDirectory!!)
		return ImpactedTestEngineConfiguration(testDataWriter, testEngineRegistry, testSorter, teamscaleAgentNotifier)
	}

	private fun createTestSorter(): ITestSorter {
		return if (!runImpacted) NOPTestSorter() else ImpactedTestsSorter(createImpactedTestsProvider())
	}

	private fun createImpactedTestsProvider(): ImpactedTestsProvider {
		val client = TeamscaleClient(
			serverOptions?.url,
			serverOptions?.userName!!,
			serverOptions?.userAccessToken!!,
			serverOptions?.project!!,
			File(reportDirectory, "server-request.txt")
		)
		return ImpactedTestsProvider(
			client, baseline!!, baselineRevision!!, endCommit!!, endRevision!!, repository!!, partition!!,
			runAllTests, includeAddedTests, includeFailedAndSkipped
		)
	}

	private fun createTeamscaleAgentNotifier() =
		TeamscaleAgentNotifier(testwiseCoverageAgentApis, runImpacted && !runAllTests)

	/** Builder for [TestEngineOptions]. */
	class Builder {
		private val options = TestEngineOptions()

		fun serverOptions(config: ServerOptions?): Builder =
			apply { options.serverOptions = config }
		fun partition(partition: String?): Builder =
			apply { options.partition = partition }
		fun repository(repository: String?): Builder =
			apply { options.repository = repository }
		fun runImpacted(flag: Boolean): Builder =
			apply { options.runImpacted = flag }
		fun runAllTests(flag: Boolean): Builder =
			apply { options.runAllTests = flag }
		fun includeAddedTests(flag: Boolean): Builder =
			apply { options.includeAddedTests = flag }
		fun includeFailedAndSkipped(flag: Boolean): Builder =
			apply { options.includeFailedAndSkipped = flag }
		fun baseline(baseline: String?): Builder =
			apply { options.baseline = baseline }
		fun baselineRevision(revision: String?): Builder =
			apply { options.baselineRevision = revision }
		fun endCommit(commit: CommitDescriptor?): Builder =
			apply { options.endCommit = commit }
		fun endRevision(revision: String?): Builder =
			apply { options.endRevision = revision }
		fun includedTestEngineIds(ids: List<String>): Builder =
			apply { options.includedTestEngineIds = ids.toSet() }
		fun excludedTestEngineIds(ids: List<String>): Builder =
			apply { options.excludedTestEngineIds = ids.toSet() }
		fun reportDirectory(path: String?): Builder =
			apply { path?.let { options.reportDirectory = File(it) } }

		fun testCoverageAgentUrls(urls: List<String>): Builder = apply {
			options.testwiseCoverageAgentApis = urls.mapNotNull {
				HttpUrl.parse(it)?.let(ITestwiseCoverageAgentApi::createService)
			}
		}

		/** Validates and builds the [TestEngineOptions]. */
		fun build(): TestEngineOptions {
			options.validateBuildPreconditions()
			return options
		}
	}

	/** Helper for build validation */
	private fun validateBuildPreconditions() {
		require(endCommit != null || endRevision != null) { "End commit must be set via endCommit or endRevision." }
		if (runImpacted) {
			requireNotNull(serverOptions) { "Server options must be set." }
		}
		requireNotNull(reportDirectory) { "Report directory must be set." }
		reportDirectory?.let {
			if (!it.isDirectory || !it.canWrite()) {
				it.toPath().createDirectories()
			}
		}
	}
}
