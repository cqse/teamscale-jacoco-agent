package com.teamscale.tia.client;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import spark.Request;
import spark.Response;
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
import static spark.Spark.exception;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;

/**
 * Mocks a Teamscale server: returns predetermined impacted tests and stores all uploaded {@link
 * TestwiseCoverageReport}s so test can run assertions on them.
 */
public class TeamscaleMockServer {

	private final JsonAdapter<List<PrioritizableTestCluster>> testClusterJsonAdapter = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, PrioritizableTestCluster.class));
	private final JsonAdapter<TestwiseCoverageReport> testwiseCoverageReportJsonAdapter = new Moshi.Builder().build()
			.adapter(TestwiseCoverageReport.class);

	/** All {@link TestwiseCoverageReport}s uploaded to this Teamscale instance. */
	public final List<TestwiseCoverageReport> uploadedReports = new ArrayList<>();
	private final Path tempDir = Files.createTempDirectory("TeamscaleMockServer");
	private final List<String> impactedTests;

	public TeamscaleMockServer(int port, String... impactedTests) throws IOException {
		this.impactedTests = Arrays.asList(impactedTests);
		port(port);
		post("/p/:projectName/external-report/", this::handleReport);
		put("/p/:projectName/test-impact", this::handleImpactedTests);
		exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
		notFound((Request request, Response response) -> {
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
		TestwiseCoverageReport report = testwiseCoverageReportJsonAdapter.fromJson(reportString);

		uploadedReports.add(report);
		file.delete();

		return "success";
	}

}
