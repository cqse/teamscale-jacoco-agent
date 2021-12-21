package com.teamscale.client;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.teamscale.client.ETestImpactOptions.ENSURE_PROCESSED;
import static com.teamscale.client.ETestImpactOptions.INCLUDE_FAILED_AND_SKIPPED;
import static com.teamscale.client.ETestImpactOptions.INCLUDE_NON_IMPACTED;

/** Helper class to interact with Teamscale. */
public class TeamscaleClient {

	/** Teamscale service implementation. */
	public final ITeamscaleService service;

	/** The project ID within Teamscale. */
	private final String projectId;

	/** Constructor with parameters for read and write timeout in seconds. */
	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId, int readTimeout, int writeTimeout) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createService(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken, readTimeout, writeTimeout);
	}

	/** Constructor. */
	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createService(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken, HttpUtils.DEFAULT_READ_TIMEOUT, HttpUtils.DEFAULT_WRITE_TIMEOUT);
	}

	/** Constructor with parameters for read and write timeout in seconds and logfile. */
	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId, File logfile, int readTimeout, int writeTimeout) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createServiceWithRequestLogging(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken,
						logfile, readTimeout, writeTimeout);
	}

	/** Constructor with logfile. */
	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId, File logfile) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createServiceWithRequestLogging(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken,
						logfile, HttpUtils.DEFAULT_READ_TIMEOUT, HttpUtils.DEFAULT_WRITE_TIMEOUT);
	}

	/**
	 * Tries to retrieve the impacted tests from Teamscale. This should be used in a CI environment, because it ensures
	 * that the given commit has been processed by Teamscale and also considers previous failing tests for
	 * re-execution.
	 *
	 * @param availableTests A list of tests that is locally available for execution. This allows TIA to consider newly
	 *                       added tests in addition to those that are already known and allows to filter e.g. if the
	 *                       user has already selected a subset of relevant tests. This can be <code>null</code> to
	 *                       indicate that only tests known to Teamscale should be suggested.
	 * @param baseline       The baseline timestamp AFTER which changes should be considered. Changes that happened
	 *                       exactly at the baseline will be excluded. In case you want to retrieve impacted tests for a
	 *                       single commit with a known timestamp you can append a <code>"p1"</code> suffix to the
	 *                       timestamp to indicate that you are interested in the changes that happened after the parent
	 *                       of the given commit.
	 * @param endCommit      The last commit for which changes should be considered.
	 * @param partitions     The partitions that should be considered for retrieving impacted tests. Can be
	 *                       <code>null</code> to indicate that tests from all partitions should be returned.
	 *
	 * @return A list of test clusters to execute. If availableTests is null, a single dummy cluster is returned with
	 * all prioritized tests.
	 */
	public Response<List<PrioritizableTestCluster>> getImpactedTests(
			List<ClusteredTestDetails> availableTests, String baseline,
			CommitDescriptor endCommit,
			List<String> partitions,
			boolean includeNonImpacted) throws IOException {

		if (includeNonImpacted) {
			return getImpactedTests(availableTests, baseline, endCommit, partitions, INCLUDE_NON_IMPACTED,
					ENSURE_PROCESSED, INCLUDE_FAILED_AND_SKIPPED);
		} else {
			return getImpactedTests(availableTests, baseline, endCommit, partitions, ENSURE_PROCESSED,
					INCLUDE_FAILED_AND_SKIPPED);
		}
	}

	/**
	 * Tries to retrieve the impacted tests from Teamscale. Use this method if you want to query time range based or you
	 * want to exclude failed and skipped tests from previous test runs.
	 *
	 * @param availableTests A list of tests that is locally available for execution. This allows TIA to consider newly
	 *                       added tests in addition to those that are already known and allows to filter e.g. if the
	 *                       user has already selected a subset of relevant tests. This can be <code>null</code> to
	 *                       indicate that only tests known to Teamscale should be suggested.
	 * @param baseline       The baseline timestamp AFTER which changes should be considered. Changes that happened
	 *                       exactly at the baseline will be excluded. In case you want to retrieve impacted tests for a
	 *                       single commit with a known timestamp you can append a <code>"p1"</code> suffix to the
	 *                       timestamp to indicate that you are interested in the changes that happened after the parent
	 *                       of the given commit.
	 * @param endCommit      The last commit for which changes should be considered.
	 * @param partitions     The partitions that should be considered for retrieving impacted tests. Can be
	 *                       <code>null</code> to indicate that tests from all partitions should be returned.
	 * @param options A list of options (See {@link ETestImpactOptions} for more details)
	 * @return A list of test clusters to execute. If availableTests is null, a single dummy cluster is returned with
	 * all prioritized tests.
	 */
	public Response<List<PrioritizableTestCluster>> getImpactedTests(
			List<ClusteredTestDetails> availableTests, String baseline,
			CommitDescriptor endCommit,
			List<String> partitions,
			ETestImpactOptions... options) throws IOException {
		EnumSet<ETestImpactOptions> testImpactOptions = EnumSet.copyOf(Arrays.asList(options));
		boolean includeNonImpacted = testImpactOptions.contains(INCLUDE_NON_IMPACTED);
		boolean includeFailedAndSkippedTests = testImpactOptions.contains(INCLUDE_FAILED_AND_SKIPPED);
		boolean ensureProcessed = testImpactOptions.contains(ENSURE_PROCESSED);

		if (availableTests == null) {
			return wrapInCluster(
					service.getImpactedTests(projectId, baseline, endCommit, partitions,
							includeNonImpacted,
							includeFailedAndSkippedTests,
							ensureProcessed)
							.execute());
		} else {
			return service
					.getImpactedTests(projectId, baseline, endCommit, partitions,
							includeNonImpacted,
							includeFailedAndSkippedTests,
							ensureProcessed, availableTests)
					.execute();
		}
	}

	private static Response<List<PrioritizableTestCluster>> wrapInCluster(
			Response<List<PrioritizableTest>> testListResponse) {
		if (testListResponse.isSuccessful()) {
			return Response.success(
					Collections.singletonList(new PrioritizableTestCluster("dummy", testListResponse.body())),
					testListResponse.raw());
		} else {
			return Response.error(testListResponse.errorBody(), testListResponse.raw());
		}
	}

	/** Uploads multiple reports to Teamscale in the given {@link EReportFormat}. */
	public void uploadReports(EReportFormat reportFormat, Collection<File> reports, CommitDescriptor commitDescriptor,
							  String revision,
							  String partition, String message) throws IOException {
		uploadReports(reportFormat.name(), reports, commitDescriptor, revision, partition, message);
	}

	/** Uploads multiple reports to Teamscale. */
	public void uploadReports(String reportFormat, Collection<File> reports, CommitDescriptor commitDescriptor,
							  String revision,
							  String partition, String message) throws IOException {
		List<MultipartBody.Part> partList = reports.stream().map(file -> {
			RequestBody requestBody = RequestBody.create(MultipartBody.FORM, file);
			return MultipartBody.Part.createFormData("report", file.getName(), requestBody);
		}).collect(Collectors.toList());

		Response<ResponseBody> response = service
				.uploadExternalReports(projectId, reportFormat, commitDescriptor, revision, true, partition, message,
						partList).execute();
		if (!response.isSuccessful()) {
			throw new IOException("HTTP request failed: " + HttpUtils.getErrorBodyStringSafe(response));
		}
	}

	/** Uploads one in-memory report to Teamscale. */
	public void uploadReport(EReportFormat reportFormat, String report, CommitDescriptor commitDescriptor,
							 String revision, String partition, String message) throws IOException {
		RequestBody requestBody = RequestBody.create(MultipartBody.FORM, report);
		service.uploadReport(projectId, commitDescriptor, revision, partition, reportFormat, message, requestBody);
	}
}
