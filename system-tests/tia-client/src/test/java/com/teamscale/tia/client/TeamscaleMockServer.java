package com.teamscale.tia.client;

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
import java.util.List;

import static java.util.stream.Collectors.joining;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static spark.Spark.exception;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;

public class TeamscaleMockServer {

	public final List<String> uploadedReports = new ArrayList<>();
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
		String testsArray = impactedTests.stream().map(uniformPath -> "{\"uniformPath\":\"" + uniformPath + "\"}")
				.collect(joining(",", "[", "]"));
		return "[{\"clusterId\":\"cluster\",\"tests\":" + testsArray + "}]";
	}

	private String handleReport(Request request, Response response) throws IOException, ServletException {
		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(tempDir.toString());
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

		Part file = request.raw().getPart("report");
		String report = IOUtils.toString(file.getInputStream());
		uploadedReports.add(normalizeTestWiseReport(report));
		file.delete();

		return "success";
	}

	private String normalizeTestWiseReport(String report) {
		return report.replaceAll("\"duration\":[^,]*,", "");
	}

}
