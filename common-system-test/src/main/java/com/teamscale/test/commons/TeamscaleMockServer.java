package com.teamscale.test.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.teamscale.client.JsonUtils;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.ProfilerConfiguration;
import com.teamscale.client.ProfilerRegistration;
import com.teamscale.client.TestWithClusterId;
import com.teamscale.report.compact.TeamscaleCompactCoverageReport;
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
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

	/** A list of all commits to which an upload happened. Can either be branch:timestamp or revision */
	public final List<String> uploadCommits = new ArrayList<>();
	/** A list of all commits for which impacted tests were requested. Can either be branch:timestamp or revision */
	public final List<String> impactedTestCommits = new ArrayList<>();
	/** A list of all commits used as baseline for retrieving impacted tests */
	public final List<String> baselines = new ArrayList<>();
	/** A list of all repositories to which an upload happened. */
	public final List<String> uploadRepositories = new ArrayList<>();
	/** A list of all repositories for which impacted tests were requested. */
	public final List<String> impactedTestRepositories = new ArrayList<>();

	/** All tests that the test engine has signaled to Teamscale as being available for execution. */
	public final Set<TestWithClusterId> allAvailableTests = new HashSet<>();
	private final Path tempDir = Files.createTempDirectory("TeamscaleMockServer");
	private final Service service;
	private List<String> impactedTests;
	private final List<String> profilerEvents = new ArrayList<>();
	private ProfilerConfiguration profilerConfiguration;
	private String username = null;
	private String accessToken = null;
	private final Map<String, Session> sessions = new HashMap<>();

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

	/** Enables authentication for the mock server by setting the provided username and access token. */
	public TeamscaleMockServer withAuthentication(String username, String accessToken) {
		this.username = username;
		this.accessToken = accessToken;
		return this;
	}

	/** Configures the server to accept report uploads and store them within the mock for later retrieval. */
	public TeamscaleMockServer acceptingReportUploads() {
		service.post("api/v5.9.0/projects/:projectId/external-analysis/session/:sessionId/report",
				this::handleReport);
		service.post("api/v5.9.0/projects/:projectId/external-analysis/session",
				this::createSession);
		service.post("api/v5.9.0/projects/:projectId/external-analysis/session/:sessionId",
				this::commitSession);
		return this;
	}

	private Object commitSession(Request request, Response response) {
		requireAuthentication(request, response);
		sessions.remove(request.params("sessionId"));
		return "";
	}

	private Object createSession(Request request, Response response) throws JsonProcessingException {
		requireAuthentication(request, response);

		String sessionId = UUID.randomUUID().toString();
		sessions.put(sessionId, new Session(request));
		return JsonUtils.serialize(sessionId);
	}

	/** Configures the server to answer all impacted test calls with the given tests. */
	public TeamscaleMockServer withImpactedTests(String... impactedTests) {
		this.impactedTests = Arrays.asList(impactedTests);
		service.put("api/v9.4.0/projects/:projectName/impacted-tests", this::handleImpactedTests);
		return this;
	}

	/** Configures the server to answer all impacted test calls with the given tests. */
	public TeamscaleMockServer withProfilerConfiguration(ProfilerConfiguration profilerConfiguration) {
		this.profilerConfiguration = profilerConfiguration;
		service.post("api/v2024.7.0/profilers", this::handleProfilerRegistration);
		service.put("api/v2024.7.0/profilers/:profilerId", this::handleProfilerHeartbeat);
		service.delete("api/v2024.7.0/profilers/:profilerId", this::handleProfilerUnregister);
		service.post("api/v2024.7.0/profilers/:profilerId/logs", this::handleProfilerLogs);
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

	/**
	 * Returns the report at the given index in {@link #uploadedReports}, parsed as a
	 * {@link TeamscaleCompactCoverageReport}.
	 *
	 * @throws IOException when parsing the report fails.
	 */
	public TeamscaleCompactCoverageReport parseUploadedCompactCoverageReport(int index) throws IOException {
		return JsonUtils.deserialize(uploadedReports.get(index).getReportString(),
				TeamscaleCompactCoverageReport.class);
	}

	private String handleImpactedTests(Request request, Response response) throws IOException {
		requireAuthentication(request, response);

		collectedUserAgents.add(request.headers("User-Agent"));
		impactedTestCommits.add(request.queryParams("end-revision") + ", " + request.queryParams("end"));
		impactedTestRepositories.add(request.queryParams("repository"));
		baselines.add(request.queryParams("baseline-revision") + ", " + request.queryParams("baseline"));
		List<TestWithClusterId> availableTests = JsonUtils.deserializeList(request.body(), TestWithClusterId.class);
		allAvailableTests.addAll(availableTests);
		Map<Optional<String>, List<TestWithClusterId>> impactedTestsByCluster = availableTests.stream()
				.filter(availableTest -> impactedTests.contains(availableTest.getTestName())).collect(
						Collectors.groupingBy(testWithClusterId -> Optional.ofNullable(testWithClusterId.getClusterId())));
		List<PrioritizableTestCluster> testClusters = new ArrayList<>();
		impactedTestsByCluster.forEach(
				(clusterId, impactedTests) -> testClusters.add(new PrioritizableTestCluster(clusterId.orElse(null),
						impactedTests.stream().map(TestWithClusterId::getTestName).map(PrioritizableTest::new)
								.collect(toList()))));
		return JsonUtils.serialize(testClusters);
	}

	private String handleProfilerRegistration(Request request, Response response) throws JsonProcessingException {
		requireAuthentication(request, response);

		collectedUserAgents.add(request.headers("User-Agent"));
		profilerEvents.add(
				"Profiler registered and requested configuration " + request.queryParams("configuration-id"));
		ProfilerRegistration registration = new ProfilerRegistration();
		registration.profilerConfiguration = this.profilerConfiguration;
		registration.profilerId = "123";
		return JsonUtils.serialize(registration);
	}

	private String handleProfilerHeartbeat(Request request, Response response) {
		requireAuthentication(request, response);

		collectedUserAgents.add(request.headers("User-Agent"));
		profilerEvents.add("Profiler " + request.params(":profilerId") + " sent heartbeat");
		return "";
	}

	private String handleProfilerLogs(Request request, Response response) {
		requireAuthentication(request, response);

		collectedUserAgents.add(request.headers("User-Agent"));
		profilerEvents.add("Profiler " + request.params(":profilerId") + " sent logs");
		return "";
	}

	private String handleProfilerUnregister(Request request, Response response) {
		requireAuthentication(request, response);

		collectedUserAgents.add(request.headers("User-Agent"));
		profilerEvents.add("Profiler " + request.params(":profilerId") + " unregistered");
		return "foo";
	}

	public List<String> getProfilerEvents() {
		return profilerEvents;
	}

	private String handleReport(Request request, Response response) throws IOException, ServletException {
		requireAuthentication(request, response);

		String sessionId = request.params(":sessionId");
		Session session;
		if (sessionId.equals("auto-create")) {
			session = new Session(request);
		} else {
			session = sessions.get(sessionId);
		}

		collectedUserAgents.add(request.headers("User-Agent"));
		uploadCommits.add(session.revision + ", " + session.commit);
		uploadRepositories.add(session.repository);
		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(tempDir.toString());
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

		Collection<Part> parts = request.raw().getParts();
		String partition = session.partition;
		String format = request.queryParams("format");
		for (Part part : parts) {
			String reportString = IOUtils.toString(part.getInputStream());
			uploadedReports.add(new ExternalReport(reportString, partition, format));
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

	private void requireAuthentication(Request request, Response response) {
		if (username != null && accessToken != null) {
			String authHeader = request.headers("Authorization");
			if (authHeader == null || !authHeader.equals(buildBasicAuthHeader(username, accessToken))) {
				response.status(401);
				throw new IllegalArgumentException("Unauthorized");
			}
		}
	}

	private String buildBasicAuthHeader(String username, String accessToken) {
		String credentials = username + ":" + accessToken;
		return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
	}

	private static class Session {

		private final String partition;
		private final String revision;
		private final String commit;
		private final String repository;

		public Session(Request request) {
			partition = request.queryParams("partition");
			revision = request.queryParams("revision");
			commit = request.queryParams("t");
			repository = request.queryParams("repository");
		}
	}
}
