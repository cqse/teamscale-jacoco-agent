package eu.cqse.teamscale.jacoco.client;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.JaCoCo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import io.reactivex.schedulers.Schedulers;

/**
 * Connects to a running JaCoCo and regularly dumps coverage to XML files on
 * disk.
 */
public class Main {

	/** Version of this program. */
	private static final String VERSION;

	static {
		ResourceBundle bundle = ResourceBundle.getBundle("eu.cqse.teamscale.jacoco.client.app");
		VERSION = bundle.getString("version");
	}

	/**
	 * Logger used to mark messages that occur due to the application being
	 * restarted. We use this to filter these messages in case restarts happen
	 * frequently and are expected to have cleaner logs.
	 */
	private static final Marker RECONNECT_LOGGER_MARKER = MarkerManager.getMarker("RECONNECT");

	/** The parsed command line arguments */
	private CommandLineArguments arguments = new CommandLineArguments();

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

		main.parseCommandLine(args);
		main.loop();
	}

	/**
	 * Parses the given command line arguments. Exits the program or throws an
	 * exception if the arguments are not valid.
	 */
	private void parseCommandLine(String[] args) throws IOException {
		JCommander jCommander = JCommander.newBuilder().programName(Main.class.getName()).addObject(arguments).build();
		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jCommander.usage();
			System.exit(1);
		}

		arguments.validate();
	}

	/** Constructor. */
	public Main() {
		logger.info("Starting JaCoCo client " + VERSION + " compiled against JaCoCo " + JaCoCo.VERSION);
	}

	/**
	 * Executes {@link #run()} in a loop to ensure this stays running even when
	 * exceptions occur.
	 * 
	 * Handles the following error cases:
	 * <ul>
	 * <li>Application is not running: wait for
	 * {@link CommandLineArguments#getReconnectIntervalInSeconds()} then retry
	 * <li>Fatal error: immediately restart
	 * </ul>
	 */
	private void loop() {
		logger.info("Connecting to JaCoCo on localhost:{} and dumping coverage every {} minutes to {}",
				arguments.getPort(), arguments.getDumpIntervalInMinutes(), arguments.getOutputDir());
		if (!arguments.getLocationIncludeFilters().isEmpty() || !arguments.getLocationExcludeFilters().isEmpty()) {
			logger.warn("Class file filters are enabled. Includes: {}, excludes: {}",
					StringUtils.concat(arguments.getLocationIncludeFilters(), ", "),
					StringUtils.concat(arguments.getLocationExcludeFilters(), ", "));
		}

		converter = new XmlReportGenerator(CollectionUtils.map(arguments.getClassDirectoriesOrZips(), File::new),
				arguments.getLocationIncludeFilters(), arguments.getLocationExcludeFilters(),
				arguments.isShouldIgnoreDuplicateClassFiles());
		store = new TimestampedFileStore(Paths.get(arguments.getOutputDir()));

		while (true) {
			try {
				run();
			} catch (ConnectException e) {
				logger.error(RECONNECT_LOGGER_MARKER,
						"Could not connect to JaCoCo. The application appears not to be running."
								+ " Trying to reconnect in {} seconds",
						arguments.getReconnectIntervalInSeconds(), e);
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(arguments.getReconnectIntervalInSeconds()));
				} catch (InterruptedException e2) {
					// ignore, retry early
				}
			} catch (Throwable t) {
				logger.error("Fatal error", t);
			}
			logger.info(RECONNECT_LOGGER_MARKER, "Restarting");
		}
	}

	/**
	 * Connects to JaCoCo and regularly dumps data. Will not return unless a fatal
	 * exception occurs.
	 */
	private void run() throws IOException, InterruptedException, ExecutionException {
		try (IJacocoController controller = new JacocoRemoteTCPController("localhost", arguments.getPort())) {
			controller.connect().doOnNext(data -> {
				logger.info("Received dump, converting");
			}).map(converter::convert).doOnNext(data -> {
				logger.info("Storing XML");
				// access to the store must be synchronized, so use the single scheduler
			}).observeOn(Schedulers.single()).subscribe(store::store, error -> {
				logger.error("Fatal exception in execution data pipeline", error);
				restart();
			}, () -> {
				logger.info("Target application shut down. Restarting and waiting for new connection.");
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
	private void scheduleRegularDump(IJacocoController controller) {
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
		}, arguments.getDumpIntervalInMinutes(), arguments.getDumpIntervalInMinutes(), TimeUnit.MINUTES);
	}

	/**
	 * Stops the dump job. This will make the {@link #run()} method exit, which
	 * causes the {@link #loop()} method to restart it.
	 */
	private void restart() {
		dumpJob.cancel(false);
	}

}
