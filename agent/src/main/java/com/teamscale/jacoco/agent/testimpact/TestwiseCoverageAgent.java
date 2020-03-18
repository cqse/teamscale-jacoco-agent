/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.RevisionInfo;
import com.teamscale.report.testwise.model.TestExecution;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class TestwiseCoverageAgent extends AgentBase {

	/** Path parameter placeholder used in the HTTP requests. */
	private static final String TEST_ID_PARAMETER = ":testId";

	/** JSON adapter for test executions. */
	private final JsonAdapter<TestExecution> testExecutionJsonAdapter = new Moshi.Builder().build()
			.adapter(TestExecution.class);

	/** JSON adapter for revision information. */
	private final JsonAdapter<RevisionInfo> revisionInfoJsonAdapter = new Moshi.Builder().build()
			.adapter(RevisionInfo.class);

	/** JSON adapter for test details. */
	private final JsonAdapter<List<ClusteredTestDetails>> clusteredTestDetailsAdapter = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, ClusteredTestDetails.class));

	private final TestEventHandlerStrategyBase testEventHandler;

	public TestwiseCoverageAgent(AgentOptions options, TestExecutionWriter testExecutionWriter,
								 JaCoCoTestwiseReportGenerator reportGenerator) throws IllegalStateException {
		super(options);

		if (options.shouldDumpCoverageViaHttp()) {
			testEventHandler = new CoverageViaHttpStrategy(controller, options, reportGenerator);
		} else if (options.shouldUploadTestWiseCoverageToTeamscale()) {
			testEventHandler = new CoverageToTeamscaleStrategy(controller, options, reportGenerator);
		} else {
			testEventHandler = new CoverageToExecFileStrategy(controller, options, testExecutionWriter);
		}
	}

	@Override
	protected void initServerEndpoints() {
		get("/test", (request, response) -> controller.getSessionId());
		get("/revision", (request, response) -> this.getRevisionInfo());
		post("/test/start/" + TEST_ID_PARAMETER, this::handleTestStart);
		post("/test/end/" + TEST_ID_PARAMETER, this::handleTestEnd);
		post("/testrun/start", this::handleTestRunStart);
		post("/testrun/end", this::handleTestRunEnd);
		exception(Exception.class, this::handleThrowable);
	}

	private void handleThrowable(Exception exception, Request request, Response response) {
		logger.error("Request to {} failed with an exception", request.pathInfo(), exception);

		// we want to print stack traces to make it easier to debug problems in the agent. Otherwise, end users
		// only see "500 - Internal server error". This is especially cumbersome when talking to the agent through
		// the TIA Java library
		response.status(SC_INTERNAL_SERVER_ERROR);
		StringWriter stringWriter = new StringWriter();
		try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
			exception.printStackTrace(printWriter);
		}
		response.body("Request failed with an exception in the agent: " + exception.getMessage() + "\n" + stringWriter
				.toString());
	}

	private String handleTestRunStart(Request request, Response response) throws IOException {
		boolean includeNonImpactedTests = "true".equalsIgnoreCase(request.params("includeNonImpacted"));

		String baselineParameter = request.params("baseline");
		Long baseline = null;
		if (baselineParameter != null) {
			try {
				baseline = Long.parseLong(baselineParameter);
			} catch (NumberFormatException e) {
				logger.error("Invalid value for parameter 'baseline'", e);
				response.status(SC_BAD_REQUEST);
				return "Invalid value for parameter 'baseline': " + e.getMessage();
			}
		}

		String bodyString = request.body();
		List<ClusteredTestDetails> availableTests;
		try {
			availableTests = clusteredTestDetailsAdapter.fromJson(bodyString);
		} catch (IOException e) {
			logger.error("Invalid request body. Expected a JSON list of ClusteredTestDetails", e);
			response.status(SC_BAD_REQUEST);
			return "Invalid request body. Expected a JSON list of ClusteredTestDetails: " + e.getMessage();
		}

		String responseBody = testEventHandler.testRunStart(availableTests, includeNonImpactedTests, baseline);
		response.type(APPLICATION_JSON.asString());
		return responseBody;
	}

	private String handleTestRunEnd(Request request, Response response) throws IOException {
		testEventHandler.testRunEnd();
		response.status(SC_NO_CONTENT);
		return "";
	}

	/** Handles the start of a new test case by setting the session ID. */
	private String handleTestStart(Request request, Response response) {
		String testId = request.params(TEST_ID_PARAMETER);
		if (testId == null || testId.isEmpty()) {
			logger.error("Test name missing in " + request.url() + "!");

			response.status(SC_BAD_REQUEST);
			return "Test name is missing!";
		}

		logger.debug("Start test " + testId);

		testEventHandler.testStart(testId);
		response.status(SC_NO_CONTENT);
		return "";
	}

	/** Handles the end of a test case by resetting the session ID. */
	private String handleTestEnd(Request request, Response response) throws DumpException, CoverageGenerationException {
		String testId = request.params(TEST_ID_PARAMETER);
		if (testId == null || testId.isEmpty()) {
			logger.error("Test name missing in " + request.url() + "!");

			response.status(SC_BAD_REQUEST);
			return "Test name is missing!";
		}

		logger.debug("End test " + testId);
		Optional<TestExecution> testExecution = getTestExecution(testId, request.body());

		String body = testEventHandler.testEnd(testId, testExecution.orElse(null));
		if (body == null) {
			response.status(SC_NO_CONTENT);
			body = "";
		} else {
			response.type(APPLICATION_JSON.asString());
			response.status(SC_OK);
		}
		return body;
	}

	/** Extracts a test execution object from the body if one is given. */
	private Optional<TestExecution> getTestExecution(String testId, String body) {
		if (body.isEmpty()) {
			return Optional.empty();
		}
		try {
			TestExecution testExecution = testExecutionJsonAdapter.fromJson(body);
			if (testExecution == null) {
				logger.error("Given request body for /test/end deserialized to null: " + body);
				return Optional.empty();
			}
			testExecution.setUniformPath(testId);
			return Optional.of(testExecution);
		} catch (IOException e) {
			logger.error("Failed to store test execution: " + e.getMessage(), e);
			return Optional.empty();
		}
	}

	/** Returns revision information for the Teamscale upload. */
	private String getRevisionInfo() {
		TeamscaleServer server = options.getTeamscaleServerOptions();
		return revisionInfoJsonAdapter.toJson(new RevisionInfo(server.commit, server.revision));
	}
}
