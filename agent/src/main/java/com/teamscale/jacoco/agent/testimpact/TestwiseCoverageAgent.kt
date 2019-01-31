/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.AgentOptions;
import com.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import spark.Request;
import spark.Response;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.stop;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class TestwiseCoverageAgent extends AgentBase {

	/** Path parameter placeholder used in the http requests. */
	private static final String TEST_ID_PARAMETER = ":testId";

	/** The agent options. */
	private AgentOptions options;

	/** Constructor. */
	public TestwiseCoverageAgent(AgentOptions options) throws IllegalStateException {
		super(options);
		this.options = options;
		initServer();
	}

	/**
	 * Starts the http server, which waits for information about started and finished tests.
	 */
	private void initServer() {
		logger.info("Listening for test events on port {}.", options.getHttpServerPort());
		port(options.getHttpServerPort());

		get("/test", (request, response) -> controller.getSessionId());

		post("/test/start/" + TEST_ID_PARAMETER, (request, response) -> {
			handleTestStart(request);
			return "success";
		});

		post("/test/end/" + TEST_ID_PARAMETER, (request, response) -> {
			handleTestEnd(request);
			return "success";
		});
	}

	/** Handles the start of a new test case by setting the session ID. */
	private void handleTestStart(Request request) {
		logger.debug("Start test " + request.params(TEST_ID_PARAMETER));

		// Dump and reset coverage so that we only record coverage that belongs to this particular test case.
		controller.reset();
		String testId = request.params(TestwiseCoverageAgent.TEST_ID_PARAMETER);
		controller.setSessionId(testId);
	}

	/** Handles the end of a test case by resetting the session ID. */
	private void handleTestEnd(Request request) throws DumpException {
		logger.debug("End test " + request.params(TEST_ID_PARAMETER));
		controller.dump();
	}

	@Override
	protected void prepareShutdown() {
		stop();
	}
}
