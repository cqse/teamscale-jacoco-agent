package com.teamscale.client;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Helper class to interact with Teamscale. */
public class TeamscaleClient {

	/** Teamscale service implementation. */
	private final ITeamscaleService service;

	/** The project ID within Teamscale. */
	private final String projectId;

	/** Constructor. */
	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createService(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken);
	}

	/**
	 * Tries to retrieve the impacted tests from Teamscale.
	 *
	 * @return A list of external IDs to execute or null in case Teamscale did not find a test details upload for the given commit.
	 */
	public Response<List<TestForPrioritization>> getImpactedTests(List<TestDetails> testList, Long baseline, CommitDescriptor endCommit, String partition) throws IOException {
		if (baseline == null) {
			return service
					.getImpactedTests(projectId, endCommit, partition, testList)
					.execute();
		} else {
			return service
					.getImpactedTests(projectId, baseline, endCommit, partition, testList)
					.execute();
		}
	}

	/** Uploads multiple reports to Teamscale. */
	public void uploadReports(EReportFormat reportFormat, Collection<File> reports, CommitDescriptor commitDescriptor, String partition, String message) throws IOException {
		List<MultipartBody.Part> partList = reports.stream().map(file -> {
			RequestBody requestBody = RequestBody.create(MultipartBody.FORM, file);
			return MultipartBody.Part.createFormData("report", file.getName(), requestBody);
		}).collect(Collectors.toList());

		Response<ResponseBody> response = service
				.uploadExternalReports(projectId, reportFormat, commitDescriptor, true, true, partition, message,
						partList).execute();
		if (!response.isSuccessful()) {
			throw new IOException(response.errorBody().string());
		}
	}
}
