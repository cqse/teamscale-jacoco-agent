/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent;

import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.upload.UploaderException;
import com.teamscale.jacoco.agent.util.Benchmark;
import com.teamscale.jacoco.agent.util.Timer;
import com.teamscale.report.jacoco.CoverageFile;
import com.teamscale.report.jacoco.EmptyReportException;
import com.teamscale.report.jacoco.JaCoCoXmlReportGenerator;
import com.teamscale.report.jacoco.dump.Dump;
import spark.Request;
import spark.Response;
import spark.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.time.Duration;

import static com.teamscale.jacoco.agent.util.LoggingUtils.wrap;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and XML conversion based on a time
 * interval.
 */
public class Agent extends AgentBase {

	/** Converts binary data to XML. */
	private JaCoCoXmlReportGenerator generator;

	/** Regular dump task. */
	private Timer timer;

	/** Stores the XML files. */
	protected final IUploader uploader;

	/** Constructor. */
	public Agent(AgentOptions options,
				 Instrumentation instrumentation) throws IllegalStateException, UploaderException {
		super(options);

		uploader = options.createUploader(instrumentation);
		logger.info("Upload method: {}", uploader.describe());

		generator = new JaCoCoXmlReportGenerator(options.getClassDirectoriesOrZips(),
				options.getLocationIncludeFilter(),
				options.getDuplicateClassFileBehavior(), options.shouldIgnoreUncoveredClasses(), wrap(logger));

		if (options.shouldDumpInIntervals()) {
			timer = new Timer(this::dumpReport, Duration.ofMinutes(options.getDumpIntervalInMinutes()));
			timer.start();
			logger.info("Dumping every {} minutes.", options.getDumpIntervalInMinutes());
		}
		if (options.getTeamscaleServerOptions().partition != null) {
			controller.setSessionId(options.getTeamscaleServerOptions().partition);
		}
	}

	@Override
	protected void initServerEndpoints(Service spark) {
		super.initServerEndpoints(spark);
		spark.post("/dump", this::handleDump);
		spark.post("/reset", this::handleReset);
	}

	/** Handles dumping a XML coverage report for coverage collected until now. */
	private String handleDump(Request request, Response response) {
		logger.debug("Dumping report triggered via HTTP request");
		dumpReport();
		response.status(HttpServletResponse.SC_NO_CONTENT);
		return "";
	}

	/** Handles resetting of coverage. */
	private String handleReset(Request request, Response response) {
		logger.debug("Resetting coverage triggered via HTTP request");
		controller.reset();
		response.status(HttpServletResponse.SC_NO_CONTENT);
		return "";
	}

	@Override
	protected void prepareShutdown() {
		if (timer != null) {
			timer.stop();
		}
		if (options.shouldDumpOnExit()) {
			dumpReport();
		}

		try {
			com.teamscale.jacoco.agent.util.FileSystemUtils.deleteDirectoryIfEmpty(options.getOutputDirectory());
		} catch (IOException e) {
			logger.info("Could not delete empty output directory {}. " +
							"This directory was created inside the configured output directory to be able to " +
							"distinguish between different runs of the profiled JVM. You may delete it manually.",
					options.getOutputDirectory(), e);
		}
	}

	/**
	 * Dumps the current execution data, converts it, writes it to the output directory defined in {@link #options} and
	 * uploads it if an uploader is configured. Logs any errors, never throws an exception.
	 */
	private void dumpReport() {
		logger.debug("Starting dump");

		try {
			dumpReportUnsafe();
		} catch (Throwable t) {
			// we want to catch anything in order to avoid crashing the whole system under test
			logger.error("Dump job failed with an exception", t);
		}
	}

	private void dumpReportUnsafe() {
		Dump dump;
		try {
			dump = controller.dumpAndReset();
		} catch (JacocoRuntimeController.DumpException e) {
			logger.error("Dumping failed, retrying later", e);
			return;
		}

		try (Benchmark ignored = new Benchmark("Generating the XML report")) {
			File outputFile = options.createTempFile("jacoco", "xml");
			CoverageFile coverageFile = generator.convert(dump, outputFile);
			uploader.upload(coverageFile);
		} catch (IOException e) {
			logger.error("Converting binary dump to XML failed", e);
		} catch (EmptyReportException e) {
			logger.warn("No coverage was collected.", e);
		}
	}
}
