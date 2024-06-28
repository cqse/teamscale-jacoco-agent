package com.teamscale.client

import com.teamscale.client.TeamscaleServiceGenerator.createServiceWithRequestLogging
import com.teamscale.client.utils.HttpUtils
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
open class TeamscaleClient @JvmOverloads constructor(
	private val config: ServerConfiguration,
	logfile: File? = null,
	readTimeout: Duration = HttpUtils.DEFAULT_READ_TIMEOUT,
	writeTimeout: Duration = HttpUtils.DEFAULT_WRITE_TIMEOUT
) {
	/** Teamscale service implementation.  */
	val service: ITeamscaleService = config.url.toHttpUrlOrNull()?.let {
		createServiceWithRequestLogging(
			it, config.userName, config.userAccessToken, logfile, readTimeout, writeTimeout
		)
	} ?: throw IllegalArgumentException("Invalid URL: ${config.url}")

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
	 * @param endCommit      The last commit for which changes should be considered.
	 * @param partitions     The partitions that should be considered for retrieving impacted tests. Can be
	 * `null` to indicate that tests from all partitions should be returned.
	 * @return A list of test clusters to execute. If [availableTests] is null, a single dummy cluster is returned with
	 * all prioritized tests.
	 */
	@Throws(IOException::class)
	open fun getImpactedTests(
		availableTests: List<ClusteredTestDetails>?,
		baseline: String?,
		endCommit: CommitDescriptor?,
		partitions: List<String>?,
		includeNonImpacted: Boolean,
		includeAddedTests: Boolean,
		includeFailedAndSkipped: Boolean
	): Response<List<PrioritizableTestCluster>> {
		val selectedOptions = mutableListOf(ETestImpactOptions.ENSURE_PROCESSED).apply {
			if (includeNonImpacted) add(ETestImpactOptions.INCLUDE_NON_IMPACTED)
			if (includeAddedTests) add(ETestImpactOptions.INCLUDE_ADDED_TESTS)
			if (includeFailedAndSkipped) add(ETestImpactOptions.INCLUDE_FAILED_AND_SKIPPED)
		}
		return getImpactedTests(
			availableTests, baseline, endCommit, partitions, selectedOptions
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
	 * @param endCommit      The last commit for which changes should be considered.
	 * @param partitions     The partitions that should be considered for retrieving impacted tests. Can be
	 * `null` to indicate that tests from all partitions should be returned.
	 * @param options        A list of options (See [ETestImpactOptions] for more details)
	 * @return A list of test clusters to execute. If availableTests is null, a single dummy cluster is returned with
	 * all prioritized tests.
	 */
	@Throws(IOException::class)
	private fun getImpactedTests(
		availableTests: List<ClusteredTestDetails>?,
		baseline: String?,
		endCommit: CommitDescriptor?,
		partitions: List<String>?,
		options: List<ETestImpactOptions>
	): Response<List<PrioritizableTestCluster>> {
		val includeNonImpacted = options.contains(ETestImpactOptions.INCLUDE_NON_IMPACTED)
		val includeFailedAndSkipped = options.contains(ETestImpactOptions.INCLUDE_FAILED_AND_SKIPPED)
		val ensureProcessed = options.contains(ETestImpactOptions.ENSURE_PROCESSED)
		val includeAddedTests = options.contains(ETestImpactOptions.INCLUDE_ADDED_TESTS)

		return if (availableTests == null) {
			wrapInCluster(
				service.getImpactedTests(
					config.project, baseline, endCommit, partitions,
					includeNonImpacted,
					includeFailedAndSkipped,
					ensureProcessed, includeAddedTests
				).execute()
			)
		} else {
			service.getImpactedTests(
				config.project, baseline, endCommit, partitions,
				includeNonImpacted,
				includeFailedAndSkipped,
				ensureProcessed,
				includeAddedTests,
				availableTests.map { clusteredTestDetails ->
					TestWithClusterId.fromClusteredTestDetails(
						clusteredTestDetails
					)
				}
			).execute()
		}
	}

	/** Uploads multiple reports to Teamscale in the given [ReportFormat]. */
	@Throws(IOException::class)
	open fun uploadReports(
		reportFormat: ReportFormat,
		reports: Collection<File>,
		commitDescriptor: CommitDescriptor?,
		revision: String?,
		partition: String,
		message: String
	) {
		uploadReports(reportFormat.name, reports, commitDescriptor, revision, partition, message)
	}

	/** Uploads multiple reports to Teamscale. */
	@Throws(IOException::class)
	open fun uploadReports(
		reportFormat: String?,
		reports: Collection<File>,
		commitDescriptor: CommitDescriptor?,
		revision: String?,
		partition: String,
		message: String
	) {
		val partList = reports.map { file ->
			val requestBody = file.asRequestBody(FORM)
			MultipartBody.Part.createFormData("report", file.name, requestBody)
		}

		val response = service.uploadExternalReports(
			config.project,
			reportFormat,
			commitDescriptor,
			revision,
			true,
			partition,
			message,
			partList
		).execute()

		if (!response.isSuccessful) {
			throw IOException("HTTP request failed: " + HttpUtils.getErrorBodyStringSafe(response))
		}
	}

	/** Uploads one in-memory report to Teamscale.  */
	@Throws(IOException::class)
	open fun uploadReport(
		reportFormat: ReportFormat,
		report: String,
		commitDescriptor: CommitDescriptor?,
		revision: String?,
		partition: String?,
		message: String?
	) {
		val requestBody = report.toRequestBody(FORM)
		service.uploadReport(config.project, commitDescriptor, revision, partition, reportFormat, message, requestBody)
	}

	companion object {
		private fun wrapInCluster(
			testListResponse: Response<List<PrioritizableTest?>?>
		) = if (testListResponse.isSuccessful) {
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
				testListResponse.errorBody()!!, testListResponse.raw()
			)
		}
	}
}
