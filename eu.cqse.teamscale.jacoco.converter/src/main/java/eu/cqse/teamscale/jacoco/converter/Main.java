package eu.cqse.teamscale.jacoco.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.logging.ILogger;
import org.conqat.lib.commons.logging.SimpleLogger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import io.reactivex.schedulers.Schedulers;

/**
 * Connects to a running JaCoCo and regularly dumps coverage to XML files on
 * disk.
 */
public class Main {

	/** The logger. */
	private final ILogger logger = new SimpleLogger();;

	/** The scheduler for the recurring dump task. */
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	/** Connection to JaCoCo. */
	private IJacocoController controller;

	/** Converts binary execution data to XML. */
	private XmlReportGenerator converter;

	/** Permanently stores the XMLs. */
	private IXmlStore store;

	/** The currently running dump task. */
	private ScheduledFuture<?> dumpJob;

	/** Entry point. */
	public static void main(String[] args) throws Exception {
		Arguments arguments = new Arguments();
		JCommander.newBuilder().addObject(args).build().parse(args);
		validate(arguments);

		new Main().loop(arguments);
	}

	/** Makes sure the arguments are valid. */
	private static void validate(Arguments arguments) throws IOException {
		for (String path : arguments.classDirectoriesOrZips) {
			CCSMAssert.isTrue(new File(path).exists(), "Path '" + path + "' does not exist");
			CCSMAssert.isTrue(new File(path).canRead(), "Path '" + path + "' is not readable");
		}

		FileSystemUtils.ensureDirectoryExists(new File(arguments.outputDir));
	}

	/**
	 * Executes {@link #run(Arguments)} in a loop to ensure this stays running even
	 * when exceptions occur.
	 */
	private void loop(Arguments arguments) {
		converter = new XmlReportGenerator(CollectionUtils.map(arguments.classDirectoriesOrZips, File::new));
		store = new TimestampedFileStore(Paths.get(arguments.outputDir), logger);

		while (true) {
			try {
				run(arguments);
			} catch (Throwable t) {
				logger.error("Fatal error", t);
			}
			logger.info("Restarting");
		}
	}

	/**
	 * Connects to JaCoCo and regularly dumps data. Will not return unless a fatal
	 * exception occurs.
	 */
	private void run(Arguments arguments) throws IOException, InterruptedException, ExecutionException {
		controller = new JacocoRemoteTCPController("localhost", arguments.port);
		controller.connect().observeOn(Schedulers.single()).doOnNext(data -> {
			logger.info("Received dump, converting");
		}).map(converter::convert).doOnNext(data -> {
			logger.info("Storing XML");
		}).subscribe(store::store, error -> {
			logger.error("Fatal exception in execution data pipeline", error);
			restart();
		});

		scheduleRegularDump(arguments);

		logger.info("Started");
		try {
			dumpJob.get();
		} catch (CancellationException e) {
			// only happens when the job was cancelled. allow restart to happen
		} finally {
			controller.close();
		}
	}

	/**
	 * Schedules a job to run regularly and dump execution data.
	 */
	private void scheduleRegularDump(Arguments arguments) throws InterruptedException, ExecutionException {
		dumpJob = executor.scheduleAtFixedRate(() -> {
			try {
				controller.dump(true);
			} catch (IOException e) {
				logger.error("Failed to dump execution data. Will retry next interval", e);
			}
		}, arguments.dumpIntervalInMinutes, arguments.dumpIntervalInMinutes, TimeUnit.MINUTES);
	}

	/**
	 * Stops the dump job. This will make the {@link #run(Arguments)} method exit.
	 */
	private void restart() {
		dumpJob.cancel(false);
	}

	/** The command line arguments. */
	private static class Arguments {

		/** The directories and/or zips that contain all class files being profiled. */
		@Parameter(names = { "--classDir", "--jar", "-c" }, required = true, description = ""
				+ "The directories or zip/ear/jar/war/... files that contain the compiled Java classes being profiled."
				+ " Searches recursively, including inside zips.")
		private List<String> classDirectoriesOrZips = new ArrayList<>();

		/** The JaCoCo port. */
		@Parameter(names = { "--port", "-p" }, required = true, description = ""
				+ "The port under which JaCoCo is listening for connections.")
		private int port = 0;

		/** The directory to write the XML traces to. */
		@Parameter(names = { "--out", "-o" }, required = true, description = ""
				+ "The directory to write the generated XML reports to.")
		private String outputDir = "";

		/** The interval in minutes for dumping XML data. */
		@Parameter(names = { "--interval", "-i" }, required = true, description = ""
				+ "Interval in minutes after which the current coverage is retrived and stored in a new XML file.")
		private int dumpIntervalInMinutes = 0;

	}

}
