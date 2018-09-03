package eu.cqse.teamscale.client;

import com.google.gson.Gson;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Helper class to interact with Teamscale. */
public class TeamscaleClient {

	/** Maximum number of times to retry getting the impacted tests from Teamscale. */
	private static final int MAX_RETRY = 100;

	/** Teamscale service implementation. */
	private final ITeamscaleService service;

	/** The project ID within Teamscale.  */
	private final String projectId;

	/** Constructor. */
	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createService(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken);
	}

	/** Uploads multiple reports to Teamscale. */
	public void uploadReports(EReportFormat reportFormat, Collection<File> reports, CommitDescriptor commitDescriptor, String partition, String message) throws IOException {
		List<MultipartBody.Part> partList = reports.stream().map(file -> {
			RequestBody requestBody = RequestBody.create(MultipartBody.FORM, file);
			return MultipartBody.Part.createFormData("report", file.getName(), requestBody);
		}).collect(Collectors.toList());

		Response<ResponseBody> response = service.uploadExternalReports(projectId, reportFormat, commitDescriptor, true,
				partition, message, partList)
				.execute();
		if (!response.isSuccessful()) {
			throw new IOException(response.errorBody().string());
		}
	}

	/** Uploads the given test details to Teamscale. */
	public void uploadTestList(List<TestDetails> list, CommitDescriptor commitDescriptor, String partition, String message) throws IOException {
		Gson gson = new Gson();
		RequestBody requestFile = RequestBody.create(MultipartBody.FORM, gson.toJson(list));
		service.uploadReport(projectId, commitDescriptor, partition, EReportFormat.TEST_LIST, message, requestFile);
	}

	/**
	 * Tries to retrieve the impacted tests from Teamscale.
	 *
	 * @return A list of external IDs to execute or null in case Teamscale did not find a test details upload for the given commit.
	 */
	public Response<List<String>> getImpactedTests(CommitDescriptor baseline, CommitDescriptor endCommit, String partition, PrintWriter out) throws IOException {
		out.print("Getting impacted tests");
		Response<List<String>> impactedTests;
		int attempts = 0;
		do {
			if (attempts > 1) {
				out.print(".");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			impactedTests = service
					.getImpactedTests(projectId, "", baseline, endCommit, partition)
					.execute();
			attempts++;
			if (attempts > MAX_RETRY) {
				return impactedTests;
			}
		} while (impactedTests.isSuccessful() && impactedTests.body() == null);
		out.println();
		return impactedTests;
	}
}
