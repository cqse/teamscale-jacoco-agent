/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.AgentOptions;
import com.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import com.teamscale.report.testwise.model.TestExecution;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class TestwiseCoverageAgent extends AgentBase {

	/** Path parameter placeholder used in the http requests. */
	private static final String TEST_ID_PARAMETER = ":testId";

	/** JSON adapter for test executions. */
	private final JsonAdapter<TestExecution> testExecutionJsonAdapter = new Moshi.Builder().build()
			.adapter(TestExecution.class);


	/** Helper for writing test executions to disk. */
	private final TestExecutionWriter testExecutionWriter;

	/** The timestamp at which the /test/start endpoint has been called last time. */
	private long startTimestamp;

	/** Constructor. */
	public TestwiseCoverageAgent(AgentOptions options,
								 TestExecutionWriter testExecutionWriter) throws IllegalStateException {
		super(options);
		this.testExecutionWriter = testExecutionWriter;
	}

	@Override
	protected void initServerEndpoints() {
		get("/test", (request, response) -> controller.getSessionId());

		post("/test/start/" + TEST_ID_PARAMETER, this::handleTestStart);
		post("/test/end/" + TEST_ID_PARAMETER, this::handleTestEnd);
	}

	/** Handles the start of a new test case by setting the session ID. */
	private String handleTestStart(Request request, Response response) {
		String testId = request.params(TEST_ID_PARAMETER);
		if (testId == null || testId.isEmpty()) {
			logger.error("Test name missing in " + request.url() + "!");

			response.status(400);
			return "Test name is missing!";
		}

		logger.debug("Start test " + testId);

		// Dump and reset coverage so that we only record coverage that belongs to this particular test case.
		controller.reset();
		controller.setSessionId(testId);
		startTimestamp = System.currentTimeMillis();

		response.status(204);
		return "";
	}

	/** Handles the end of a test case by resetting the session ID. */
	private String handleTestEnd(Request request, Response response) throws DumpException {
		String testId = request.params(TEST_ID_PARAMETER);
		if (testId == null || testId.isEmpty()) {
			logger.error("Test name missing in " + request.url() + "!");

			response.status(400);
			return "Test name is missing!";
		}

		logger.debug("End test " + testId);
		controller.dump();

		// Test execution is optional
		if (!request.body().isEmpty()) {
			try {
				TestExecution testExecution = testExecutionJsonAdapter.fromJson(request.body());
				if (testExecution == null) {
					response.status(400);
					return "Test execution may not be null!";
				}
				testExecution.setUniformPath(testId);
				long endTimestamp = System.currentTimeMillis();
				testExecution.setDurationMillis(endTimestamp - startTimestamp);
				testExecutionWriter.append(testExecution);
			} catch (IOException e) {
				logger.error("Failed to store test execution: " + e.getMessage(), e);
			}
		}

		response.status(204);
		return "";
	}
}
