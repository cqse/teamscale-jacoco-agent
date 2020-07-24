package com.teamscale.client;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Helper class to interact with Teamscale. */
public class TeamscaleClient {

	/** Teamscale service implementation. */
	public final ITeamscaleService service;

	/** The project ID within Teamscale. */
	private final String projectId;

	/** Constructor. */
	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createService(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken);
	}

	/** Constructor. */
	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId, File file) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createServiceWithRequestLogging(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken,
						file);
	}

	/**
	 * Tries to retrieve the impacted tests from Teamscale.
	 *
	 * @return A list of test clusters to execute. If availableTests is null, a single dummy cluster is returned with
	 * all prioritized tests.
	 */
	public Response<List<PrioritizableTestCluster>> getImpactedTests(
			List<ClusteredTestDetails> availableTests, Long baseline,
			CommitDescriptor endCommit,
			String partition,
			boolean includeNonImpacted) throws IOException {

		if (baseline == null) {
			if (availableTests == null) {
				return wrapInCluster(
						service.getImpactedTests(projectId, endCommit, partition, includeNonImpacted).execute());
			} else {
				return service.getImpactedTests(projectId, endCommit, partition, includeNonImpacted, availableTests)
						.execute();
			}
		} else {
			if (availableTests == null) {
				return wrapInCluster(
						service.getImpactedTests(projectId, baseline, endCommit, partition, includeNonImpacted)
								.execute());
			} else {
				return service
						.getImpactedTests(projectId, baseline, endCommit, partition, includeNonImpacted, availableTests)
						.execute();
			}
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

	/** Uploads multiple reports to Teamscale. */
	public void uploadReports(EReportFormat reportFormat, Collection<File> reports, CommitDescriptor commitDescriptor,
							  String partition, String message) throws IOException {
		List<MultipartBody.Part> partList = reports.stream().map(file -> {
			RequestBody requestBody = RequestBody.create(MultipartBody.FORM, file);
			return MultipartBody.Part.createFormData("report", file.getName(), requestBody);
		}).collect(Collectors.toList());

		Response<ResponseBody> response = service
				.uploadExternalReports(projectId, reportFormat, commitDescriptor, true, partition, message,
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
