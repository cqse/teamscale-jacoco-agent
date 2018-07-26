package eu.cqse.teamscale.client;

import com.google.gson.Gson;
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

@SuppressWarnings({"WeakerAccess", "unused"})
public class TeamscaleClient {
	private static final int MAX_RETRY = 100;

	private final ITeamscaleService service;
	private final String projectId;

	public TeamscaleClient(Server server) {
		this(server.url, server.userName, server.userAccessToken, server.project);
	}

	public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId) {
		this.projectId = projectId;
		service = TeamscaleServiceGenerator
				.createService(ITeamscaleService.class, HttpUrl.parse(baseUrl), user, accessToken);
	}

	public void uploadReports(EReportFormat reportFormat, Collection<File> reports, CommitDescriptor commitDescriptor, String partition, String message) throws IOException {
		uploadReportBodys(reportFormat, commitDescriptor, partition, message, reports);
	}

	private void uploadReportBodys(EReportFormat reportFormat, CommitDescriptor commitDescriptor, String partition, String message, Collection<File> files) throws IOException {
		System.out.println("Uploading reports to " + commitDescriptor.toString() + " (" + partition + ")");
		List<MultipartBody.Part> partList = files.stream().map(file -> {
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

	public void uploadReport(EReportFormat reportFormat, String report, CommitDescriptor commitDescriptor, String partition, String message) throws IOException {
		RequestBody requestFile = RequestBody.create(MultipartBody.FORM, report);
		uploadReportBody(reportFormat, commitDescriptor, partition, message, requestFile);
	}

	private void uploadReportBody(EReportFormat reportFormat, CommitDescriptor commitDescriptor, String partition, String message, RequestBody requestFile) throws IOException {
		System.out.println("Uploading report to " + commitDescriptor.toString() + " (" + partition + ")");
		service.uploadReport(projectId, commitDescriptor, partition, reportFormat, message, requestFile);
	}

	public void uploadTestList(List<TestDetails> list, CommitDescriptor commitDescriptor, String partition, String message) throws IOException {
		Gson gson = new Gson();
		RequestBody requestFile = RequestBody.create(MultipartBody.FORM, gson.toJson(list));
		uploadReportBody(EReportFormat.TEST_LIST, commitDescriptor, partition, message, requestFile);
	}

	public Response<List<String>> getImpactedTests(CommitDescriptor commitDescriptor, String partition) throws IOException {
		System.out.print("Getting impacted tests");
		Response<List<String>> impactedTests;
		int attempts = 0;
		do {
			if (attempts > 1) {
				System.out.print(".");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			impactedTests = service
					.getImpactedTests(projectId, "", commitDescriptor.commitBefore(), commitDescriptor, partition)
					.execute();
			attempts++;
			if (attempts > MAX_RETRY) {
				return impactedTests;
			}
		} while (impactedTests.isSuccessful() && impactedTests.body() == null);
		System.out.println();
		return impactedTests;
	}
}
