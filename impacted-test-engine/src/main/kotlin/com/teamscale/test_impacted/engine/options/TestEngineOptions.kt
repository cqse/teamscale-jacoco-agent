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
import java.io.IOException
import java.nio.file.Files

/** Represents options for the [com.teamscale.test_impacted.engine.ImpactedTestEngine].  */
class TestEngineOptions {
	/** The server options. May not be null.  */
	private var serverOptions: ServerOptions? = null

	/** The partition to upload test details to and get impacted tests from. If null, all partitions are used.
	 * @see [partition]
	 */
	var partition: String? = null
		private set

	/** Executes all tests, not only impacted ones if set. Defaults to false.
	 * @see [runAllTests]
	 */
	private var runAllTests = false

	/** Executes only impacted tests, not all ones if set. Defaults to true.  */
	private var runImpacted = true

	/** Includes added tests in the list of tests to execute. Defaults to true
	 * @see [includeAddedTests]
	 */
	private var includeAddedTests = true

	/** Includes failed and skipped tests in the list of tests to execute. Defaults to true
	 * @see [includeFailedAndSkipped]
	 */
	private var includeFailedAndSkipped = true

	/**
	 * The baseline. Only code changes after the baseline are considered for determining impacted tests. May be null to
	 * indicate no baseline.
	 */
	private var baseline: String? = null

	/**
	 * Can be used instead of [baseline] by using a revision (e.g. git SHA1) instead of a branch and timestamp.
	 */
	private var baselineRevision: String? = null

	/** The end commit used for TIA and for uploading the coverage. May not be null.  */
	private var endCommit: CommitDescriptor? = null

	/**
	 * Can be used instead of [endCommit] by using a revision (e.g. git SHA1) instead of a branch and timestamp.
	 */
	private var endRevision: String? = null

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	private var repository: String? = null

	/** The URLs (including port) at which the agents listen to. Maybe empty but not null.  */
	private var testwiseCoverageAgentApis = emptyList<ITestwiseCoverageAgentApi>()

	/** The test engine ids of all [org.junit.platform.engine.TestEngine]s to use.
	 * If empty all available [org.junit.platform.engine.TestEngine]s are used.  */
	private var includedTestEngineIds = emptySet<String>()

	/** The test engine ids of all [org.junit.platform.engine.TestEngine]s to exclude.  */
	private var excludedTestEngineIds = emptySet<String>()

	/** The directory used to store test-wise coverage reports. Must be a writeable directory.  */
	private var reportDirectory: File? = null

	fun createTestEngineConfiguration(): ImpactedTestEngineConfiguration {
		val testSorter = createTestSorter()
		val teamscaleAgentNotifier = createTeamscaleAgentNotifier()
		val testEngineRegistry = TestEngineRegistry(includedTestEngineIds, excludedTestEngineIds)
		val testDataWriter = TestDataWriter(reportDirectory)

		return ImpactedTestEngineConfiguration(testDataWriter, testEngineRegistry, testSorter, teamscaleAgentNotifier)
	}

	private fun createTestSorter(): ITestSorter {
		if (!runImpacted) {
			return NOPTestSorter()
		}

		val testsProvider = createImpactedTestsProvider()
		return ImpactedTestsSorter(testsProvider)
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

	/** The builder for [TestEngineOptions].  */
	class Builder {
		private val testEngineOptions = TestEngineOptions()

		fun serverOptions(serverOptions: ServerOptions?): Builder {
			testEngineOptions.serverOptions = serverOptions
			return this
		}

		fun partition(partition: String?): Builder {
			testEngineOptions.partition = partition
			return this
		}

		fun runImpacted(runImpacted: Boolean): Builder {
			testEngineOptions.runImpacted = runImpacted
			return this
		}

		fun runAllTests(runAllTests: Boolean): Builder {
			testEngineOptions.runAllTests = runAllTests
			return this
		}

		fun includeAddedTests(includeAddedTests: Boolean): Builder {
			testEngineOptions.includeAddedTests = includeAddedTests
			return this
		}

		fun includeFailedAndSkipped(includeFailedAndSkipped: Boolean): Builder {
			testEngineOptions.includeFailedAndSkipped = includeFailedAndSkipped
			return this
		}

		fun endCommit(endCommit: CommitDescriptor?): Builder {
			testEngineOptions.endCommit = endCommit
			return this
		}

		fun endRevision(endRevision: String?): Builder {
			testEngineOptions.endRevision = endRevision
			return this
		}

		fun repository(repository: String?): Builder {
			testEngineOptions.repository = repository
			return this
		}

		fun baseline(baseline: String?): Builder {
			testEngineOptions.baseline = baseline
			return this
		}

		fun baselineRevision(baselineRevision: String?): Builder {
			testEngineOptions.baselineRevision = baselineRevision
			return this
		}

		fun agentUrls(agentUrls: List<String>): Builder {
			testEngineOptions.testwiseCoverageAgentApis = agentUrls
				.map { HttpUrl.parse(it) }
				.mapNotNull {
					if (it != null) {
						ITestwiseCoverageAgentApi.createService(it)
					} else null
				}
			return this
		}

		fun includedTestEngineIds(testEngineIds: List<String>): Builder {
			testEngineOptions.includedTestEngineIds = HashSet(testEngineIds)
			return this
		}

		fun excludedTestEngineIds(testEngineIds: List<String>): Builder {
			testEngineOptions.excludedTestEngineIds = HashSet(testEngineIds)
			return this
		}

		fun reportDirectory(reportDirectory: String?): Builder {
			reportDirectory ?: return this
			testEngineOptions.reportDirectory = File(reportDirectory)
			return this
		}

		/** Checks field conditions and returns the built [TestEngineOptions].  */
		fun build(): TestEngineOptions {
			if (testEngineOptions.endCommit == null && testEngineOptions.endRevision == null) {
				throw AssertionError("End commit must be set via endCommit or endRevision.")
			}
			if (testEngineOptions.runImpacted) {
				checkNotNull(testEngineOptions.serverOptions) { "Server options must be set." }
			}
			checkNotNull(testEngineOptions.testwiseCoverageAgentApis) { "Agent urls may be empty but not null." }
			checkNotNull(testEngineOptions.reportDirectory) { "Report directory must be set." }
			if (!testEngineOptions.reportDirectory!!.isDirectory || !testEngineOptions.reportDirectory!!.canWrite()) {
				try {
					Files.createDirectories(testEngineOptions.reportDirectory!!.toPath())
				} catch (e: IOException) {
					throw AssertionError(
						"Report directory could not be created: ${testEngineOptions.reportDirectory}", e
					)
				}
			}
			return testEngineOptions
		}
	}

	companion object {
		/** Returns the builder for [TestEngineOptions].  */
		@JvmStatic
		fun builder(): Builder {
			return Builder()
		}
	}
}
