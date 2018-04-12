/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2017 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.watch;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.string.StringUtils;

import eu.cqse.teamscale.jacoco.client.IXmlStore;
import eu.cqse.teamscale.jacoco.client.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.client.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.client.util.Timer;
import io.reactivex.schedulers.Schedulers;

/**
 * Watches a JaCoCo instance via a TCP port and regularly dumps XML coverage.
 */
public class Watcher {

	/**
	 * Logger used to mark messages that occur due to the application being
	 * restarted. We use this to filter these messages in case restarts happen
	 * frequently and are expected to have cleaner logs.
	 */
	private static final Marker RECONNECT_LOGGER_MARKER = MarkerManager.getMarker("RECONNECT");

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** The parsed command line arguments. */
	private final WatchCommand arguments;

	/** The recurring dump task. */
	private Timer timer;

	/** Converts binary execution data to XML. */
	private final XmlReportGenerator converter;

	/** Permanently stores the XMLs. */
	private final IXmlStore store;

	/** Constructor. */
	public Watcher(WatchCommand arguments) {
		this.arguments = arguments;
		this.converter = new XmlReportGenerator(CollectionUtils.map(arguments.getClassDirectoriesOrZips(), File::new),
				arguments.getLocationIncludeFilters(), arguments.getLocationExcludeFilters(),
				arguments.isShouldIgnoreDuplicateClassFiles());
		this.store = new TimestampedFileStore(Paths.get(arguments.getOutputDir()));
	}

	/**
	 * Executes {@link #run()} in a loop to ensure this stays running even when
	 * exceptions occur.
	 * 
	 * Handles the following error cases:
	 * <ul>
	 * <li>Application is not running: wait for
	 * {@link WatchCommand#getReconnectIntervalInSeconds()} then retry
	 * <li>Fatal error: immediately restart
	 * </ul>
	 */
	public void loop() {
		logger.info("Connecting to JaCoCo on localhost:{} and dumping coverage every {} minutes to {}",
				arguments.getPort(), arguments.getDumpIntervalInMinutes(), arguments.getOutputDir());
		if (!arguments.getLocationIncludeFilters().isEmpty() || !arguments.getLocationExcludeFilters().isEmpty()) {
			logger.warn("Class file filters are enabled. Includes: {}, excludes: {}",
					StringUtils.concat(arguments.getLocationIncludeFilters(), ", "),
					StringUtils.concat(arguments.getLocationExcludeFilters(), ", "));
		}

		while (true) {
			try {
				run();
			} catch (ConnectException e) {
				logger.error(RECONNECT_LOGGER_MARKER,
						"Could not connect to JaCoCo at " + arguments.getHost() + ":" + arguments.getPort()
								+ ". The application appears not to be running." + " Trying to reconnect in {} seconds",
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
	private void run() throws IOException {
		try (IJacocoController controller = new JacocoRemoteTCPController(arguments.getHost(), arguments.getPort())) {
			timer = new Timer(() -> dump(controller), Duration.ofMinutes(arguments.getDumpIntervalInMinutes()));

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

			logger.info("Connected successfully to JaCoCo");

			timer.start();
			timer.waitUntilTimerIsStopped();
		} catch (CancellationException e) {
			// only happens when the job was cancelled. allow restart to happen
		}
	}

	/**
	 * Performs a single dump via the controller.
	 */
	private void dump(IJacocoController controller) {
		logger.info("Requesting dump");
		try {
			controller.dump(true);
		} catch (IOException e) {
			// this means the connection is no longer up and we need to restart
			logger.error("Failed to dump execution data. Most likely, the connection to"
					+ " the application was interrupted. Will try to reconnect", e);
			restart();
		}
	}

	/**
	 * Stops the dump job. This will make the {@link #run()} method exit, which
	 * causes the {@link #loop()} method to restart it.
	 */
	private void restart() {
		timer.stop();
	}
}
