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
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static com.teamscale.jacoco.agent.util.LoggingUtils.wrap;
import static spark.Spark.get;
import static spark.Spark.post;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and XML conversion based on a time
 * interval.
 */
public class Agent extends AgentBase {

	/** Path parameter placeholder used in the http requests. */
	private static final String PARTITION_PARAMETER = ":partition";

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
				options.getDuplicateClassFileBehavior(), wrap(logger));

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
	protected void initServerEndpoints() {
		get("/partition", (request, response) ->
				Optional.ofNullable(options.getTeamscaleServerOptions().partition).orElse(""));

		post("/dump", this::handleDump);
		post("/reset", this::handleReset);
		post("/partition/" + PARTITION_PARAMETER, this::handleSetPartition);
	}

	/** Handles dumping a XML coverage report for coverage collected until now. */
	private String handleDump(Request request, Response response) {
		logger.debug("Dumping report triggered via HTTP request");
		dumpReport();
		response.status(204);
		return "";
	}

	/** Handles resetting of coverage. */
	private String handleReset(Request request, Response response) {
		logger.debug("Resetting coverage triggered via HTTP request");
		controller.reset();
		response.status(204);
		return "";
	}

	/** Handles setting the partition name. */
	private String handleSetPartition(Request request, Response response) {
		String partition = request.params(PARTITION_PARAMETER);
		if (partition == null || partition.isEmpty()) {
			logger.error("Partition missing in " + request.url() + "! Expected /partition/Some%20Partition%20Name.");

			response.status(400);
			return "Partition name is missing!";
		}

		logger.debug("Changing partition name to " + partition);
		controller.setSessionId(partition);
		options.getTeamscaleServerOptions().partition = partition;

		response.status(204);
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
			logger.info("Could not delete emtpy output directory {}. " +
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

		CoverageFile coverageFile;
		long currentTime = System.currentTimeMillis();
		Path outputPath = options.getOutputDirectory().resolve("jacoco-" + currentTime + ".xml");

		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			FileSystemUtils.ensureParentDirectoryExists(outputPath.toFile());
			coverageFile = generator.convert(dump, outputPath, false);
		} catch (IOException e) {
			logger.error("Converting binary dump to XML failed", e);
			return;
		} catch (EmptyReportException e) {
			logger.warn("No coverage was collected.", e);
			return;
		}
		uploader.upload(coverageFile);
	}
}
