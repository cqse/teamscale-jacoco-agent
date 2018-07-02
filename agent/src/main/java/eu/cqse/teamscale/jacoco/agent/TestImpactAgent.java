/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.cqse.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import eu.cqse.teamscale.jacoco.cache.CoverageGenerationException;
import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.testwise.TestwiseXmlReportGenerator;
import eu.cqse.teamscale.jacoco.util.Benchmark;

import static eu.cqse.teamscale.jacoco.util.LoggingUtils.wrap;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.stop;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class TestImpactAgent extends AgentBase {

	/** The agent options. */
	private AgentOptions options;

	/** Converts binary data to XML. */
	private TestwiseXmlReportGenerator generator;

	/** Timestamp at which the report was dumped the last time. */
	private long lastDumpTimestamp = System.currentTimeMillis();

	/** List of dumps, one for each test. */
	private List<Dump> dumps = new ArrayList<>();

	/** Constructor. */
	public TestImpactAgent(AgentOptions options) throws IllegalStateException, CoverageGenerationException {
		super(options);
		this.options = options;
		this.generator = new TestwiseXmlReportGenerator(options.getClassDirectoriesOrZips(), options.getLocationIncludeFilter(), wrap(logger));

		logger.info("Dumping every {} minutes.", options.getDumpIntervalInMinutes());

		initServer();
	}

	/**
	 * Starts the http server, which waits for information about started and finished tests.
	 */
	private void initServer() {
		logger.info("Listening for test events on port {}.", options.getHttpServerPort());
		port(options.getHttpServerPort());

		post("/test/start/:testId", (request, response) -> {
			String testId = request.params(":testId");
			handleTestStart(testId);
			return "success";
		});

		post("/test/end/:testId", (request, response) -> {
			String testId = request.params(":testId");
			handleTestEnd(testId);
			return "success";
		});
	}

	/** Handles the start of a new test case by setting the session ID. */
	private void handleTestStart(String testId) {
		logger.debug("Start test " + testId);
		// Reset coverage so that we only record coverage that belongs to this particular test case.
		// Dumps from previous tests are stored in #dumps
		controller.reset();
		controller.setSessionId(testId);
	}

	/** Handles the end of a test case by resetting the session ID. */
	private void handleTestEnd(String testId) throws DumpException {
		logger.debug("End test " + testId);
		dumps.add(controller.dumpAndReset());

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
		String xml;
		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			xml = generator.convert(dumps);
		} catch (IOException e) {
			logger.error("Converting binary dumps to XML failed", e);
			return;
		}

		store.store(xml);
		dumps.clear();
	}

	@Override
	protected void prepareShutdown() {
		stop();
		dumpReport();
	}
}
