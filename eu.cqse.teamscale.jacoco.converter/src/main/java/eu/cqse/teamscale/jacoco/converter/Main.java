package eu.cqse.teamscale.jacoco.converter;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import io.reactivex.schedulers.Schedulers;

/**
 * Connects to a running JaCoCo and regularly dumps coverage to XML files on
 * disk.
 */
public class Main {

	/** Minutes to wait after a connection drop to try and reconnect. */
	private static final int RECONNECT_SLEEP_INTERVAL_MINUTES = 5;

	/** The directories and/or zips that contain all class files being profiled. */
	@Parameter(names = { "--classDir", "--jar", "-c" }, required = true, description = ""
			+ "The directories or zip/ear/jar/war/... files that contain the compiled Java classes being profiled."
			+ " Searches recursively, including inside zips.")
	private List<String> classDirectoriesOrZips = new ArrayList<>();

	/** The directories and/or zips that contain all class files being profiled. */
	@Parameter(names = { "--filter", "-f" }, description = ""
			+ "Ant-style include patterns to apply to all locations during JaCoCo's traversal of class files."
			+ " Note that zip contents are separated from zip files with @ and that you can filter both"
			+ " class files and intermediate folders/zips. Use with great care as missing class files"
			+ " lead to broken coverage files! Turn on debug logging to see which locations are being filtered."
			+ " Defaults to no filtering.")
	private List<String> locationIncludeFilters = new ArrayList<>();

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

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** The scheduler for the recurring dump task. */
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	/** Converts binary execution data to XML. */
	private XmlReportGenerator converter;

	/** Permanently stores the XMLs. */
	private IXmlStore store;

	/** The currently running dump task. */
	private ScheduledFuture<?> dumpJob;

	/** Entry point. */
	public static void main(String[] args) throws Exception {
		Main main = new Main();

		JCommander jCommander = JCommander.newBuilder().programName(Main.class.getName()).addObject(main).build();
		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jCommander.usage();
			System.exit(1);
		}

		main.validate();
		main.loop();
	}

	/** Makes sure the arguments are valid. */
	private void validate() throws IOException {
		for (String path : classDirectoriesOrZips) {
			CCSMAssert.isTrue(new File(path).exists(), "Path '" + path + "' does not exist");
			CCSMAssert.isTrue(new File(path).canRead(), "Path '" + path + "' is not readable");
		}

		FileSystemUtils.ensureDirectoryExists(new File(outputDir));
		CCSMAssert.isTrue(new File(outputDir).canWrite(), "Path '" + outputDir + "' is not writable");
	}

	/**
	 * Executes {@link #run()} in a loop to ensure this stays running even when
	 * exceptions occur.
	 * 
	 * Handles the following error cases:
	 * <ul>
	 * <li>Application is not running: wait for one minute, then retry
	 * <li>Fatal error: immediately restart
	 * </ul>
	 */
	private void loop() {
		logger.info("Connecting to JaCoCo on localhost:{} and dumping coverage every {} minutes to {}", port,
				dumpIntervalInMinutes, outputDir);
		if (!locationIncludeFilters.isEmpty()) {
			logger.warn("Class file filters are enabled: {}", StringUtils.concat(locationIncludeFilters, ", "));
		}

		converter = new XmlReportGenerator(CollectionUtils.map(classDirectoriesOrZips, File::new),
				locationIncludeFilters);
		store = new TimestampedFileStore(Paths.get(outputDir));

		while (true) {
			try {
				run();
			} catch (ConnectException e) {
				logger.error("Could not connect to JaCoCo. The application appears not to be running."
						+ " Trying to reconnect in {} minutes", RECONNECT_SLEEP_INTERVAL_MINUTES, e);
				try {
					Thread.sleep(TimeUnit.MINUTES.toMillis(RECONNECT_SLEEP_INTERVAL_MINUTES));
				} catch (InterruptedException e2) {
					// ignore, retry early
				}
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
	private void run() throws IOException, InterruptedException, ExecutionException {
		try (IJacocoController controller = new JacocoRemoteTCPController("localhost", port)) {
			controller.connect().observeOn(Schedulers.single()).doOnNext(data -> {
				logger.info("Received dump, converting");
			}).map(converter::convert).doOnNext(data -> {
				logger.info("Storing XML");
			}).subscribe(store::store, error -> {
				logger.error("Fatal exception in execution data pipeline", error);
				restart();
			});

			scheduleRegularDump(controller);

			logger.info("Connected successfully to JaCoCo");
			// blocks until either the job throws an exception or is cancelled
			dumpJob.get();
		} catch (CancellationException e) {
			// only happens when the job was cancelled. allow restart to happen
		}
	}

	/**
	 * Schedules a job to run regularly and dump execution data.
	 */
	private void scheduleRegularDump(IJacocoController controller) throws InterruptedException, ExecutionException {
		dumpJob = executor.scheduleAtFixedRate(() -> {
			logger.info("Requesting dump");
			try {
				controller.dump(true);
			} catch (IOException e) {
				// this means the connection is no longer up and we need to restart
				logger.error("Failed to dump execution data. Most likely, the connection to"
						+ " the application was interrupted. Will try to reconnect", e);
				restart();
			}
		}, dumpIntervalInMinutes, dumpIntervalInMinutes, TimeUnit.SECONDS);
	}

	/**
	 * Stops the dump job. This will make the {@link #run()} method exit which
	 * causes the {@link #loop()} method to restart it.
	 */
	private void restart() {
		dumpJob.cancel(false);
	}

}
