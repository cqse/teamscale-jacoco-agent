/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent.testimpact;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import eu.cqse.teamscale.jacoco.agent.AgentBase;
import eu.cqse.teamscale.jacoco.agent.AgentOptions;
import eu.cqse.teamscale.jacoco.agent.ITestListener;
import eu.cqse.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService;
import eu.cqse.teamscale.jacoco.cache.CoverageGenerationException;
import eu.cqse.teamscale.jacoco.dump.Dump;
import org.apache.logging.log4j.Logger;
import spark.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.JACOCO;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.JUNIT;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.TESTWISE_COVERAGE;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.TEST_LIST;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.stop;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class TestImpactAgent extends AgentBase {

	/** Path parameter placeholder used in the http requests. */
	public static final String TEST_ID_PARAMETER = ":testId";

	/** The agent options. */
	private AgentOptions options;

	/** Timestamp at which the report was dumped the last time. */
	private long lastDumpTimestamp = System.currentTimeMillis();

	/**
	 * List of tests listeners that produce individual test artifacts.
	 * The uploads happen in the order from first to last listener.
	 */
	private final List<ITestListener> testListeners = new ArrayList<>();

	/** Constructor. */
	public TestImpactAgent(AgentOptions options) throws IllegalStateException, CoverageGenerationException {
		super(options);
		this.options = options;
		Set<ITeamscaleService.EReportFormat> reportFormats = options.getHttpServerReportFormats();
		if (reportFormats.contains(TEST_LIST)) {
			testListeners.add(new TestDetailsCollector());
		}
		if (reportFormats.contains(TESTWISE_COVERAGE)) {
			testListeners.add(new TestwiseCoverageCollector(controller, options));
		}
		if (reportFormats.contains(JUNIT)) {
			testListeners.add(new JUnitReportCollector());
		}
		if (reportFormats.contains(JACOCO)) {
			testListeners.add(new JaCoCoCoverageCollector(options));
		}

		logger.info("Collecting formats: "+reportFormats);

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

	/**
	 * Returns the test details from the request body or null if no valid test details were given.
	 */
	public static TestDetails getTestDetailsFromRequest(Request request, Logger logger) {
		String testDetailsString = request.body();
		TestDetails testDetails;
		try {
			testDetails = new Gson().fromJson(testDetailsString, TestDetails.class);
			if(testDetails == null) {
				logger.error("No or invalid test details '"+ testDetailsString +"' given!");
				return null;
			}
		} catch(JsonSyntaxException e) {
			logger.error("No or invalid test details '"+ testDetailsString + "' given!", e);
			return null;
		}
		String externalId = request.params(TEST_ID_PARAMETER);
		if (!Objects.equals(testDetails.externalId, externalId)) {
			logger.warn("The externalId '" + externalId + "' given as query parameter does not match with the " +
					"externalId '" + testDetails.externalId + "' in the test details in the request body. The " +
					"externalId from the request body is used.");
		}
		return testDetails;
	}
}
