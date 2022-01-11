package com.teamscale.client;

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
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static spark.Spark.exception;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Mocks a Teamscale server: stores all uploaded reports so tests can run assertions on them.
 */
public class TeamscaleMockServer {

	/** All reports uploaded to this Teamscale instance. */
	public final List<String> uploadedReports = new ArrayList<>();
	private final Path tempDir = Files.createTempDirectory("TeamscaleMockServer");

	public TeamscaleMockServer(int port) throws IOException {
		Service service = Service.ignite();
		service.port(port);
		service.post("api/v5.9.0/projects/:projectName/external-analysis/session/auto-create/report", this::handleReport);
		service.exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
		service.notFound((Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			return "Unexpected request: " + request.requestMethod() + " " + request.uri();
		});
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

}
