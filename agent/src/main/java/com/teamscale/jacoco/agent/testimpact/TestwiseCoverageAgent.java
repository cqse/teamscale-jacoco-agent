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
import com.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;

import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static spark.Spark.get;
import static spark.Spark.post;

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

	private final TestEventHandlerStrategyBase testEventHandler;

	/** Constructor. */
	public TestwiseCoverageAgent(AgentOptions options, IAgent agent,
								 TestExecutionWriter testExecutionWriter) throws IllegalStateException, IOException, CoverageGenerationException {
		super(options, agent);
		if (options.shouldDumpCoverageViaHttp()) {
			testEventHandler = new CoverageViaHttpStrategy(options, controller);
		} else {
			testEventHandler = new CoverageToExecFileStrategy(testExecutionWriter, controller);
		}
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
			logger.error("Test name missing in " + request.url() + "!");

			response.status(SC_BAD_REQUEST);
			return "Test name is missing!";
		}

		notifyTestStart(testId);
		logger.debug("Start test {}", testId);

		testEventHandler.testStart(testId);
		response.status(SC_NO_CONTENT);
		return "";
	}

	/** Handles the end of a test case by resetting the session ID. */
	private String handleTestEnd(Request request, Response response) throws DumpException {
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
