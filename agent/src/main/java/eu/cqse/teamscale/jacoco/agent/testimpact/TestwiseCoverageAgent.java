/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent.testimpact;

import eu.cqse.teamscale.jacoco.agent.AgentBase;
import eu.cqse.teamscale.jacoco.agent.AgentOptions;
import eu.cqse.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import spark.Request;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.stop;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class TestwiseCoverageAgent extends AgentBase {

	/** Path parameter placeholder used in the http requests. */
	public static final String TEST_ID_PARAMETER = ":testId";

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
	private void handleTestStart(Request request) throws DumpException {
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
