package com.teamscale.test_impacted.engine.executor

import com.teamscale.client.ClusteredTestDetails
import com.teamscale.client.CommitDescriptor
import com.teamscale.client.PrioritizableTestCluster
import com.teamscale.client.TeamscaleClient
import com.teamscale.test_impacted.commons.LoggerUtils.createLogger
import retrofit2.Response
import java.io.IOException
import java.util.logging.Level

/**
 * Class for retrieving the impacted [PrioritizableTestCluster]s corresponding to [ClusteredTestDetails]
 * available for test execution.
 */
open class ImpactedTestsProvider(
	private val client: TeamscaleClient,
	private val baseline: String,
	private val baselineRevision: String,
	private val endCommit: CommitDescriptor,
	private val endRevision: String,
	private val repository: String,
	val partition: String,
	private val includeNonImpacted: Boolean,
	private val includeAddedTests: Boolean,
	private val includeFailedAndSkipped: Boolean
) {
	private val logger = createLogger()
	
	/** Queries Teamscale for impacted tests.  */
	fun getImpactedTestsFromTeamscale(
		availableTestDetails: List<ClusteredTestDetails>
	): List<PrioritizableTestCluster> {
		try {
			logger.info { "Getting impacted tests..." }
			val response = client
				.getImpactedTests(
					availableTestDetails, baseline, baselineRevision, endCommit, endRevision, repository,
					listOf(partition), includeNonImpacted, includeAddedTests, includeFailedAndSkipped
				)

			if (response.isSuccessful) {
				val testClusters = response.body()
				if (testClusters != null && testCountIsPlausible(testClusters, availableTestDetails)) {
					return testClusters
				}
				logger.severe(
					"""
					Teamscale was not able to determine impacted tests:
					${response.body()}
					""".trimIndent()
				)
			} else {
				logger.severe(
					"Retrieval of impacted tests failed: ${response.code()} ${response.message()}\n${
						getErrorBody(response)
					}"
				)
			}
		} catch (e: IOException) {
			logger.log(
				Level.SEVERE, e
			) { "Retrieval of impacted tests failed." }
		}
		return emptyList()
	}

	/**
	 * Checks that the number of tests returned by Teamscale matches the number of available tests when running with
	 * [.includeNonImpacted].
	 */
	private fun testCountIsPlausible(
		testClusters: List<PrioritizableTestCluster>,
		availableTestDetails: List<ClusteredTestDetails>
	): Boolean {
		val returnedTests = testClusters.stream().mapToLong {
			it.tests?.size?.toLong() ?: 0
		}.sum()
		if (!includeNonImpacted) {
			logger.info { "Received $returnedTests impacted tests of ${availableTestDetails.size} available tests." }
			return true
		}
		if (returnedTests == availableTestDetails.size.toLong()) {
			return true
		} else {
			logger.severe {
				"Retrieved $returnedTests tests from Teamscale, but expected ${availableTestDetails.size}."
			}
			return false
		}
	}

	companion object {
		@Throws(IOException::class)
		private fun getErrorBody(response: Response<*>): String {
			response.errorBody().use { error ->
				if (error != null) {
					return error.string()
				}
			}
			return ""
		}
	}
}
