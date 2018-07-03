/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eu.cqse.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService;
import eu.cqse.teamscale.jacoco.cache.CoverageGenerationException;
import eu.cqse.teamscale.jacoco.dump.Dump;
import spark.Request;

import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.JACOCO;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.JUNIT;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.TESTWISE_COVERAGE;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.stop;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class TestImpactAgent extends AgentBase {

	/** Path parameter placeholder used in the http requests. */
	public static final String TEST_ID_PARAMETER = ":testId";

	/** The name of the query parameter that can be used to to transfer the internalId of the test. */
	public static final String INTERNAL_ID_QUERY_PARAM = "internalId";

	/** The agent options. */
	private AgentOptions options;

	/** Timestamp at which the report was dumped the last time. */
	private long lastDumpTimestamp = System.currentTimeMillis();

	/** List of tests listeners that produce individual test artifacts. */
	private final List<ITestListener> testListeners = new ArrayList<>();

	/** Constructor. */
	public TestImpactAgent(AgentOptions options) throws IllegalStateException, CoverageGenerationException {
		super(options);
		this.options = options;
		Set<ITeamscaleService.EReportFormat> reportFormats = options.getHttpServerReportFormats();
		if (reportFormats.contains(TESTWISE_COVERAGE)) {
			testListeners.add(new TestwiseCoverageListener(controller, options, logger));
		}
		if (reportFormats.contains(JUNIT)) {
			testListeners.add(new JUnitListener());
		}
		if (reportFormats.contains(JACOCO)) {
			testListeners.add(new JaCoCoCoverageListener(options, logger));
		}


		logger.info("Dumping every {} minutes.", options.getDumpIntervalInMinutes());

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

		post("/dump", (request, response) -> {
			dumpReport();
			return "success";
		});
	}

	/** Handles the start of a new test case by setting the session ID. */
	private void handleTestStart(Request request) throws DumpException {
		logger.debug("Start test " + request.params(TEST_ID_PARAMETER));
		Dump dump = controller.dumpAndReset();
		for (ITestListener testListener : testListeners) {
			testListener.onTestStart(request, dump);
		}
	}

	/** Handles the end of a test case by resetting the session ID. */
	private void handleTestEnd(Request request) throws DumpException {
		logger.debug("End test " + request.params(TEST_ID_PARAMETER));

		Dump dump = controller.dumpAndReset();
		for (ITestListener testListener : testListeners) {
			testListener.onTestFinish(request, dump);
		}

		// If the last dump was longer ago than the specified interval dump report
		if (lastDumpTimestamp + options.getDumpIntervalInMillis() < System.currentTimeMillis()) {
			dumpReport();
			lastDumpTimestamp = System.currentTimeMillis();
		}
	}

	/**
	 * Performs the actual dump but does not handle e.g. OutOfMemoryErrors.
	 */
	@Override
	protected void dumpReportUnsafe() {
		for (ITestListener testListener : testListeners) {
			testListener.onDump(store);
		}
	}

	@Override
	protected void prepareShutdown() {
		stop();
		dumpReport();
	}
}
