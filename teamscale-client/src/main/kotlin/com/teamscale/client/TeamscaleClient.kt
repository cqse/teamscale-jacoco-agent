package com.teamscale.client

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MultipartBody
import okhttp3.MultipartBody.Companion.FORM
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.time.Duration
import java.util.*

/** Helper class to interact with Teamscale.  */
open class TeamscaleClient {
	/** Teamscale service implementation.  */
	var service: ITeamscaleService

	/** The project ID within Teamscale.  */
	private val projectId: String?

	/** Constructor with parameters for read and write timeout in seconds.  */
	@JvmOverloads
	constructor(
		baseUrl: String?,
		user: String,
		accessToken: String,
		projectId: String?,
		readTimeout: Duration = HttpUtils.DEFAULT_READ_TIMEOUT,
		writeTimeout: Duration = HttpUtils.DEFAULT_WRITE_TIMEOUT
	) {
		val url = baseUrl?.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL: $baseUrl")
		this.projectId = projectId
		service = TeamscaleServiceGenerator.createService(
			ITeamscaleService::class.java, url, user, accessToken, readTimeout, writeTimeout
		)
	}

	/** Constructor with parameters for read and write timeout in seconds and logfile.  */
	@JvmOverloads
	constructor(
		baseUrl: String?,
		user: String,
		accessToken: String,
		projectId: String?,
		logfile: File?,
		readTimeout: Duration = HttpUtils.DEFAULT_READ_TIMEOUT,
		writeTimeout: Duration = HttpUtils.DEFAULT_WRITE_TIMEOUT
	) {
		val url = baseUrl?.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL: $baseUrl")
		this.projectId = projectId
		service = TeamscaleServiceGenerator.createServiceWithRequestLogging(
			ITeamscaleService::class.java, url, user, accessToken, logfile, readTimeout, writeTimeout
		)
	}

	/**
	 * Tries to retrieve the impacted tests from Teamscale. This should be used in a CI environment, because it ensures
	 * that the given commit has been processed by Teamscale and also considers previous failing tests for
	 * re-execution.
	 *
	 * @param availableTests A list of tests that is locally available for execution. This allows TIA to consider newly
	 * added tests in addition to those that are already known and allows to filter e.g. if the
	 * user has already selected a subset of relevant tests. This can be `null` to
	 * indicate that only tests known to Teamscale should be suggested.
	 * @param baseline       The baseline timestamp AFTER which changes should be considered. Changes that happened
	 * exactly at the baseline will be excluded. In case you want to retrieve impacted tests for a
	 * single commit with a known timestamp you can append a `"p1"` suffix to the
	 * timestamp to indicate that you are interested in the changes that happened after the parent
	 * of the given commit.
	 * @param baselineRevision Same as baseline but accepts a revision (e.g. git SHA1) instead of a branch and timestamp
	 * @param endCommit      The last commit for which changes should be considered.
	 * @param endRevision    Same as endCommit but accepts a revision (e.g. git SHA1) instead of a branch and timestamp
	 * @param repository     The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 * @param partitions     The partitions that should be considered for retrieving impacted tests. Can be
	 * `null` to indicate that tests from all partitions should be returned.
	 * @return A list of test clusters to execute. If availableTests is null, a single dummy cluster is returned with
	 * all prioritized tests.
	 */
	@Throws(IOException::class)
	open fun getImpactedTests(
		availableTests: List<ClusteredTestDetails>?,
		baseline: String?,
		baselineRevision: String?,
		endCommit: CommitDescriptor?,
		endRevision: String?,
		repository: String?,
		partitions: List<String>,
		includeNonImpacted: Boolean,
		includeAddedTests: Boolean,
		includeFailedAndSkipped: Boolean
	): Response<List<PrioritizableTestCluster>?> {
		val selectedOptions = mutableListOf(ETestImpactOptions.ENSURE_PROCESSED)
		if (includeNonImpacted) {
			selectedOptions.add(ETestImpactOptions.INCLUDE_NON_IMPACTED)
		}
		if (includeAddedTests) {
			selectedOptions.add(ETestImpactOptions.INCLUDE_ADDED_TESTS)
		}
		if (includeFailedAndSkipped) {
			selectedOptions.add(ETestImpactOptions.INCLUDE_FAILED_AND_SKIPPED)
		}
		return getImpactedTests(
			availableTests, baseline, baselineRevision, endCommit, endRevision, repository, partitions,
			*selectedOptions.toTypedArray<ETestImpactOptions>()
		)
	}

	/**
	 * Tries to retrieve the impacted tests from Teamscale. Use this method if you want to query time range based or you
	 * want to exclude failed and skipped tests from previous test runs.
	 *
	 * @param availableTests A list of tests that is locally available for execution. This allows TIA to consider newly
	 * added tests in addition to those that are already known and allows to filter e.g. if the
	 * user has already selected a subset of relevant tests. This can be `null` to
	 * indicate that only tests known to Teamscale should be suggested.
	 * @param baseline       The baseline timestamp AFTER which changes should be considered. Changes that happened
	 * exactly at the baseline will be excluded. In case you want to retrieve impacted tests for a
	 * single commit with a known timestamp you can append a `"p1"` suffix to the
	 * timestamp to indicate that you are interested in the changes that happened after the parent
	 * of the given commit.
	 * @param baselineRevision Same as baseline but accepts a revision (e.g. git SHA1) instead of a branch and timestamp
	 * @param endCommit      The last commit for which changes should be considered.
	 * @param endRevision    Same as endCommit but accepts a revision (e.g. git SHA1) instead of a branch and timestamp
	 * @param repository     The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 * @param partitions     The partitions that should be considered for retrieving impacted tests. Can be
	 * `null` to indicate that tests from all partitions should be returned.
	 * @param options        A list of options (See [ETestImpactOptions] for more details)
	 * @return A list of test clusters to execute. If [availableTests] is null, a single dummy cluster is returned with
	 * all prioritized tests.
	 */
	@Throws(IOException::class)
	private fun getImpactedTests(
		availableTests: List<ClusteredTestDetails>?,
		baseline: String?,
		baselineRevision: String?,
		endCommit: CommitDescriptor?,
		endRevision: String?,
		repository: String?,
		partitions: List<String>,
		vararg options: ETestImpactOptions
	): Response<List<PrioritizableTestCluster>?> {
		val testImpactOptions = EnumSet.copyOf(listOf(*options))
		val includeNonImpacted = testImpactOptions.contains(ETestImpactOptions.INCLUDE_NON_IMPACTED)
		val includeFailedAndSkipped = testImpactOptions.contains(ETestImpactOptions.INCLUDE_FAILED_AND_SKIPPED)
		val ensureProcessed = testImpactOptions.contains(ETestImpactOptions.ENSURE_PROCESSED)
		val includeAddedTests = testImpactOptions.contains(ETestImpactOptions.INCLUDE_ADDED_TESTS)

		require (projectId != null) { "Project ID must not be null!" }

		return if (availableTests == null) {
			wrapInCluster(
				service.getImpactedTests(
					projectId, baseline, baselineRevision, endCommit, endRevision, repository, partitions,
					includeNonImpacted, includeFailedAndSkipped, ensureProcessed, includeAddedTests
				).execute()
			)
		} else {
			val availableTestsMap = availableTests.map { clusteredTestDetails ->
				TestWithClusterId.fromClusteredTestDetails(clusteredTestDetails)
			}
			service.getImpactedTests(
				projectId, baseline, baselineRevision, endCommit, endRevision, repository, partitions,
				includeNonImpacted, includeFailedAndSkipped, ensureProcessed, includeAddedTests, availableTestsMap
			).execute()
		}
	}

	/** Uploads multiple reports to Teamscale in the given [EReportFormat].  */
	@Throws(IOException::class)
	open fun uploadReports(
		reportFormat: EReportFormat,
		reports: Collection<File>,
		commitDescriptor: CommitDescriptor?,
		revision: String?,
		repository: String?,
		partition: String,
		message: String
	) {
		uploadReports(reportFormat.name, reports, commitDescriptor, revision, repository, partition, message)
	}

	/** Uploads multiple reports to Teamscale.  */
	@Throws(IOException::class)
	open fun uploadReports(
		reportFormat: String,
		reports: Collection<File>,
		commitDescriptor: CommitDescriptor?,
		revision: String?,
		repository: String?,
		partition: String,
		message: String
	) {
		val partList = reports.map { file ->
			val requestBody = file.asRequestBody(FORM)
			MultipartBody.Part.createFormData("report", file.name, requestBody)
		}

		require (projectId != null) { "Project ID must not be null!" }

		val response = service
			.uploadExternalReports(
				projectId, reportFormat, commitDescriptor, revision, repository, true, partition, message, partList
			).execute()
		if (!response.isSuccessful) {
			throw IOException("HTTP request failed: " + HttpUtils.getErrorBodyStringSafe(response))
		}
	}

	/** Uploads one in-memory report to Teamscale.  */
	@Throws(IOException::class)
	open fun uploadReport(
		reportFormat: EReportFormat,
		report: String,
		commitDescriptor: CommitDescriptor?,
		revision: String?,
		repository: String?,
		partition: String,
		message: String
	) {
		require (projectId != null) { "Project ID must not be null!" }

		service.uploadReport(
			projectId,
			commitDescriptor,
			revision,
			repository,
			partition,
			reportFormat,
			message,
			report.toRequestBody(FORM)
		)
	}

	companion object {
		private fun wrapInCluster(
			testListResponse: Response<List<PrioritizableTest>>
		): Response<List<PrioritizableTestCluster>?> {
			return if (testListResponse.isSuccessful) {
				Response.success(
					listOf(
						PrioritizableTestCluster(
							"dummy",
							testListResponse.body()
						)
					),
					testListResponse.raw()
				)
			} else {
				Response.error(
					testListResponse.errorBody()!!,
					testListResponse.raw()
				)
			}
		}
	}
}
