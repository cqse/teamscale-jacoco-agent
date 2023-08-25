package com.teamscale.test.commons;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import spark.Request;
import spark.Response;
import spark.Service;
import spark.utils.IOUtils;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * Mocks a Teamscale server: returns predetermined impacted tests and stores all uploaded reports so tests can run
 * assertions on them.
 */
public class TeamscaleMockServer {

	private final JsonAdapter<List<PrioritizableTestCluster>> testClusterJsonAdapter = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, PrioritizableTestCluster.class));

	private final JsonAdapter<List<ClusteredTestDetails>> testDetailsJsonAdapter = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, ClusteredTestDetails.class));

	private final JsonAdapter<TestwiseCoverageReport> testwiseCoverageReportJsonAdapter = new Moshi.Builder().build()
			.adapter(TestwiseCoverageReport.class);

	/** All reports uploaded to this Teamscale instance. */
	public final List<ExternalReport> uploadedReports = new ArrayList<>();
	/** All user agents that were present in the received requests. */
	public final Set<String> collectedUserAgents = new HashSet<>();

	/** All tests that the test engine has signaled to Teamscale as being available for execution. */
	public final Set<ClusteredTestDetails> availableTests = new HashSet<>();
	private final Path tempDir = Files.createTempDirectory("TeamscaleMockServer");
	private final Service service;
	private final List<String> impactedTests;

	public TeamscaleMockServer(int port, boolean expectNoInteraction, String...impactedTests) throws IOException {
		this.impactedTests = Arrays.asList(impactedTests);
		service = Service.ignite();
		service.port(port);
		if (expectNoInteraction) {
			service.post("", (request, response) -> { throw new IllegalStateException("No POST to the mock server expected!"); });
			service.put("", (request, response) -> { throw new IllegalStateException("No PUT to the mock server expected!"); });
		} else {
			service.post("api/v5.9.0/projects/:projectName/external-analysis/session/auto-create/report",
					this::handleReport);
			service.put("api/v8.0.0/projects/:projectName/impacted-tests", this::handleImpactedTests);
		}
		service.exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
		service.notFound((Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			return "Unexpected request: " + request.requestMethod() + " " + request.uri();
		});
		service.awaitInitialization();
	}

	/**
	 * Returns the report at the given index in {@link #uploadedReports}, parsed as a {@link TestwiseCoverageReport}.
	 *
	 * @throws IOException when parsing the report fails.
	 */
	public TestwiseCoverageReport parseUploadedTestwiseCoverageReport(int index) throws IOException {
		return testwiseCoverageReportJsonAdapter.fromJson(uploadedReports.get(index).getReportString());
	}

	private String handleImpactedTests(Request request, Response response) throws IOException {
		collectedUserAgents.add(request.headers("User-Agent"));
		availableTests.addAll(testDetailsJsonAdapter.fromJson(request.body()));
		List<PrioritizableTest> tests = impactedTests.stream().map(PrioritizableTest::new).collect(toList());
		return testClusterJsonAdapter.toJson(Collections.singletonList(new PrioritizableTestCluster("cluster", tests)));
	}

	private String handleReport(Request request, Response response) throws IOException, ServletException {
		collectedUserAgents.add(request.headers("User-Agent"));
		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(tempDir.toString());
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

		Part file = request.raw().getPart("report");
		String partition = request.queryParams("partition");
		String reportString = IOUtils.toString(file.getInputStream());
		uploadedReports.add(new ExternalReport(reportString, partition));
		file.delete();

		return "success";
	}

	/**
	 * Shuts down the mock server and waits for it to be stopped.
	 */
	public void shutdown() {
		service.stop();
		service.awaitStop();
	}
}
