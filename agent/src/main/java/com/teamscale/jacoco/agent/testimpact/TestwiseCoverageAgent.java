/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact;

import java.io.IOException;
import org.jacoco.agent.rt.IAgent;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.AgentOptions;
import com.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import com.teamscale.report.testwise.model.TestExecution;

import spark.Request;
import spark.Response;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens
 * for test events. The agent may act as a primary or secondary agent. Its role
 * is determined at startup.
 * 
 * If the port given in the agent options is available, it acts as a primary
 * agent, accepting registrations from secondary agents. In that case, any test
 * start or test end event is forwarded to all secondary agents.
 * 
 * If the port given in the agent options is occupied, it will use the next
 * available free port for itself and try to register with a primary agent that
 * listens at the original port. Secondary agents will unregister themselves
 * with the primary agent before shutting down.
 */
public class TestwiseCoverageAgent extends AgentBase {

	/** Path parameter placeholder used in the HTTP requests. */
	private static final String TEST_ID_PARAMETER = ":testId";

	/** JSON adapter for test executions. */
	private final JsonAdapter<TestExecution> testExecutionJsonAdapter = new Moshi.Builder().build()
			.adapter(TestExecution.class);

	/** Helper for writing test executions to disk. */
	private final TestExecutionWriter testExecutionWriter;

	/** The timestamp at which the /test/start endpoint has been called last time. */
	private long startTimestamp;

	/** Constructor. */
	public TestwiseCoverageAgent(AgentOptions options, IAgent jacocoAgent,
			 TestExecutionWriter testExecutionWriter) throws IOException {
		super(options, jacocoAgent);
		this.testExecutionWriter = testExecutionWriter;
	}

	/**
	 * Starts the HTTP server, which waits for information about started and
	 * finished tests.
	 */
	@Override
	protected void initServerEndpoints() {
		service.get("/test", (request, response) -> controller.getSessionId());

		service.post("/test/start/" + TEST_ID_PARAMETER, this::handleTestStart);
		service.post("/test/end/" + TEST_ID_PARAMETER, this::handleTestEnd);
	}

	/** Handles the start of a new test case by setting the session ID. */
	private String handleTestStart(Request request, Response response) {
		String testId = request.params(TEST_ID_PARAMETER);
		if (testId == null || testId.isEmpty()) {
			return error(response, "Test name missing in {}!", request.url());
		}

		notifyTestStart(testId);
		logger.debug("Start test {}", testId);

		// Dump and reset coverage so that we only record coverage that belongs
		// to this particular test case.
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
			return error(response, "Test name missing in {}!", request.url());
		}

		notifyTestEnd(testId);
		logger.debug("End test {}", testId);

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

	/** Notifies all secondary agents about a test start event. */
	private void notifyTestStart(String testId) {
		secondaryAgents.forEach((port, agent) -> agent.signalTestStart(testId).enqueue(IAgentService
				.failureCallback(() -> logger.warn("Error signaling test start to agent on port {}", port))));
	}

	/** Notifies all secondary agents about a test end event. */
	private void notifyTestEnd(String testId) {
		secondaryAgents.forEach((port, agent) -> agent.signalTestEnd(testId).enqueue(IAgentService
				.failureCallback(() -> logger.warn("Error signaling test end to agent on port {}", port))));
	}
	
	/**
	 * Waits for the HTTP service to be initialized. Mainly used in tests to
	 * prevent them from finishing before we can work with them.
	 */
	/* package */ void awaitServiceInitialization() {
		service.awaitInitialization();
	}
}
