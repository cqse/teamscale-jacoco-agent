package com.teamscale.test.commons;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
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
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * Mocks a Teamscale server: returns predetermined impacted tests and stores all uploaded reports so tests can run
 * assertions on them.
 */
public class TeamscaleMockServer {

	private final JsonAdapter<List<PrioritizableTestCluster>> testClusterJsonAdapter = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, PrioritizableTestCluster.class));

	/** All reports uploaded to this Teamscale instance. */
	public final List<String> uploadedReports = new ArrayList<>();
	private final Path tempDir = Files.createTempDirectory("TeamscaleMockServer");
	private final Service service;
	private final List<String> impactedTests;

	public TeamscaleMockServer(int port, String... impactedTests) throws IOException {
		this.impactedTests = Arrays.asList(impactedTests);
		service = Service.ignite();
		service.port(port);
		service.post("api/v5.9.0/projects/:projectName/external-analysis/session/auto-create/report",
				this::handleReport);
		service.put("api/v6.5.2/projects/:projectName/impacted-tests", this::handleImpactedTests);
		service.exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
		service.notFound((Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			return "Unexpected request: " + request.requestMethod() + " " + request.uri();
		});
	}

	private String handleImpactedTests(Request request, Response response) {
		List<PrioritizableTest> tests = impactedTests.stream().map(PrioritizableTest::new).collect(toList());
		return testClusterJsonAdapter.toJson(Collections.singletonList(new PrioritizableTestCluster("cluster", tests)));
	}

	private String handleReport(Request request, Response response) throws IOException, ServletException {
		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(tempDir.toString());
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

		Part file = request.raw().getPart("report");
		String reportString = IOUtils.toString(file.getInputStream());
		uploadedReports.add(reportString);
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
