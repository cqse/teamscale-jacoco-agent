package com.teamscale.test.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.teamscale.client.JsonUtils;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.ProfilerConfiguration;
import com.teamscale.client.ProfilerRegistration;
import com.teamscale.client.TestWithClusterId;
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
import java.util.Collection;
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

	/** All reports uploaded to this Teamscale instance. */
	public final List<ExternalReport> uploadedReports = new ArrayList<>();
	/** All user agents that were present in the received requests. */
	public final Set<String> collectedUserAgents = new HashSet<>();

	/** All tests that the test engine has signaled to Teamscale as being available for execution. */
	public final Set<TestWithClusterId> availableTests = new HashSet<>();
	private final Path tempDir = Files.createTempDirectory("TeamscaleMockServer");
	private final Service service;
	private List<String> impactedTests;
	private final List<String> profilerEvents = new ArrayList<>();
	private ProfilerConfiguration profilerConfiguration;

	public TeamscaleMockServer(int port) throws IOException {
		service = Service.ignite();
		service.port(port);
		service.exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
		service.notFound((Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			return "Unexpected request: " + request.requestMethod() + " " + request.uri();
		});
		service.init();
		service.awaitInitialization();
	}

	/** Configures the server to accept report uploads and store them within the mock for later retrieval. */
	public TeamscaleMockServer acceptingReportUploads() {
		service.post("api/v5.9.0/projects/:projectName/external-analysis/session/auto-create/report",
				this::handleReport);
		return this;
	}

	/** Configures the server to answer all impacted test calls with the given tests. */
	public TeamscaleMockServer withImpactedTests(String... impactedTests) {
		this.impactedTests = Arrays.asList(impactedTests);
		service.put("api/v8.0.0/projects/:projectName/impacted-tests", this::handleImpactedTests);
		return this;
	}

	/** Configures the server to answer all impacted test calls with the given tests. */
	public TeamscaleMockServer withProfilerConfiguration(ProfilerConfiguration profilerConfiguration) {
		this.profilerConfiguration = profilerConfiguration;
		service.post("api/v9.4.0/running-profilers", this::handleProfilerRegistration);
		service.put("api/v9.4.0/running-profilers/:profilerId", this::handleProfilerHeartbeat);
		service.delete("api/v9.4.0/running-profilers/:profilerId", this::handleProfilerUnregister);
		return this;
	}

	/** Configures the server to answer all POST/PUT requests with an error. */
	public TeamscaleMockServer disallowingStateChanges() {
		service.post("", (request, response) -> {
			throw new IllegalStateException("No POST to the mock server expected!");
		});
		service.put("", (request, response) -> {
			throw new IllegalStateException("No PUT to the mock server expected!");
		});
		return this;
	}

	/**
	 * Returns the report at the given index in {@link #uploadedReports}, parsed as a {@link TestwiseCoverageReport}.
	 *
	 * @throws IOException when parsing the report fails.
	 */
	public TestwiseCoverageReport parseUploadedTestwiseCoverageReport(int index) throws IOException {
		return JsonUtils.deserialize(uploadedReports.get(index).getReportString(), TestwiseCoverageReport.class);
	}

	private String handleImpactedTests(Request request, Response response) throws IOException {
		collectedUserAgents.add(request.headers("User-Agent"));
		availableTests.addAll(JsonUtils.deserializeList(request.body(), TestWithClusterId.class));
		List<PrioritizableTest> tests = impactedTests.stream().map(PrioritizableTest::new).collect(toList());
		return JsonUtils.serialize(Collections.singletonList(new PrioritizableTestCluster("cluster", tests)));
	}

	private String handleProfilerRegistration(Request request, Response response) throws JsonProcessingException {
		collectedUserAgents.add(request.headers("User-Agent"));
		profilerEvents.add(
				"Profiler registered and requested configuration " + request.queryParams("configuration-id"));
		ProfilerRegistration registration = new ProfilerRegistration();
		registration.profilerConfiguration = this.profilerConfiguration;
		registration.profilerId = "123";
		return JsonUtils.serialize(registration);
	}

	private String handleProfilerHeartbeat(Request request, Response response) {
		collectedUserAgents.add(request.headers("User-Agent"));
		profilerEvents.add("Profiler " + request.params(":profilerId") + " sent heartbeat");
		return "";
	}

	private String handleProfilerUnregister(Request request, Response response) {
		collectedUserAgents.add(request.headers("User-Agent"));
		profilerEvents.add("Profiler " + request.params(":profilerId") + " unregistered");
		return "foo";
	}

	public List<String> getProfilerEvents() {
		return profilerEvents;
	}

	private String handleReport(Request request, Response response) throws IOException, ServletException {
		collectedUserAgents.add(request.headers("User-Agent"));
		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(tempDir.toString());
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

		Collection<Part> parts = request.raw().getParts();
		String partition = request.queryParams("partition");
		for (Part part : parts) {
			String reportString = IOUtils.toString(part.getInputStream());
			uploadedReports.add(new ExternalReport(reportString, partition));
			part.delete();
		}

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
