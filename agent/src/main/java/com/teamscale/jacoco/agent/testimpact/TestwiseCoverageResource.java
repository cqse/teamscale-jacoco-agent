package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.ResourceBase;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;

public class TestwiseCoverageResource extends ResourceBase {

	/** Path parameter placeholder used in the HTTP requests. */
	private static final String TEST_ID_PARAMETER = "{testId}";

	/** JSON adapter for test executions. */
	private final JsonAdapter<TestExecution> testExecutionJsonAdapter = new Moshi.Builder().build()
			.adapter(TestExecution.class);

	/** JSON adapter for test details. */
	private final JsonAdapter<List<ClusteredTestDetails>> clusteredTestDetailsAdapter = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, ClusteredTestDetails.class));

	private static TestwiseCoverageAgent TESTWISE_COVERAGE_AGENT;

	public static void setAgent(TestwiseCoverageAgent agent) {
		TestwiseCoverageResource.TESTWISE_COVERAGE_AGENT = agent;
		ResourceBase.AGENT_BASE = agent;
	}

	@GET
	@Path("/test")
	public String getTest() {
		return TESTWISE_COVERAGE_AGENT.controller.getSessionId();
	}


	@POST
	@Path("/test/start/" + TEST_ID_PARAMETER)
	public Response handleTestStart(@PathParam(TEST_ID_PARAMETER) String testId) {
		if (testId == null || testId.isEmpty()) {
			handleBadRequest("Test name is missing!");
		}

		logger.debug("Start test " + testId);

		TESTWISE_COVERAGE_AGENT.testEventHandler.testStart(testId);
		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

	@POST
	@Path("/test/end/" + TEST_ID_PARAMETER)
	public Response handleTestEnd(@PathParam(TEST_ID_PARAMETER) String testId,
								  String requestBody) throws JacocoRuntimeController.DumpException, CoverageGenerationException {
		if (testId == null || testId.isEmpty()) {
			handleBadRequest("Test name is missing!");
		}

		logger.debug("End test " + testId);
		Optional<TestExecution> testExecution = getTestExecution(testId, requestBody);

		String responseBody = TESTWISE_COVERAGE_AGENT.testEventHandler.testEnd(testId, testExecution.orElse(null));
		if (responseBody == null) {
			return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
		}
		return Response.status(SC_OK).entity(responseBody).type(APPLICATION_JSON.asString()).build();
	}

	@POST
	@Path("/testrun/start")
	public Response handleTestRunStart(@QueryParam("include-non-impacted") boolean includeNonImpactedTests,
									   @QueryParam("include-added-tests") boolean includeAddedTests,
									   @QueryParam("include-failed-and-skipped") boolean includeFailedAndSkipped,
									   @QueryParam("baseline") String baseline, String bodyString) throws IOException {

		// we explicitly allow omitting the request body. This indicates that the user doesn't want to provide
		// available tests and that Teamscale should simply use all tests it currently knows about.
		// This corresponds to the GET endpoint of the TIA service.
		List<ClusteredTestDetails> availableTests = null;
		if (!StringUtils.isEmpty(bodyString)) {
			try {
				// we explicitly allow passing null. This indicates that the user doesn't want to provide
				// available tests and that Teamscale should simply use all tests it currently knows about.
				// This corresponds to the GET endpoint of the TIA service.
				availableTests = clusteredTestDetailsAdapter.nullSafe().fromJson(bodyString);
			} catch (IOException e) {
				logger.error("Invalid request body. Expected a JSON list of ClusteredTestDetails", e);
				handleBadRequest(
						"Invalid request body. Expected a JSON list of ClusteredTestDetails: " + e.getMessage());
			}
		}

		String responseBody = TESTWISE_COVERAGE_AGENT.testEventHandler.testRunStart(availableTests,
				includeNonImpactedTests, includeAddedTests,
				includeFailedAndSkipped, baseline);
		return Response.status(SC_OK).entity(responseBody).type(APPLICATION_JSON.asString()).build();
	}

	@POST
	@Path("/testrun/end")
	public Response handleTestRunEnd() throws IOException, CoverageGenerationException {
		TESTWISE_COVERAGE_AGENT.testEventHandler.testRunEnd();
		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
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
}
