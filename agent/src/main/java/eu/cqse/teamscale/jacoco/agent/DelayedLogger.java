package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.report.util.ILogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A logger that buffers logs in memory and writes them to the actual logger at a later point.
 * This is needed when stuff needs to be logged before the actual logging framework is initialized.
 */
public class DelayedLogger implements ILogger {

	/** Buffered list of log levels. */
	private final List<Level> logLevels = new ArrayList<>();

	/** Buffered list of log messages. */
	private final List<String> logMessages = new ArrayList<>();

	/** Buffered list of logged {@link Throwable}s. */
	private final List<Throwable> logThrowables = new ArrayList<>();

	@Override
	public void debug(String message) {
		add(Level.DEBUG, message, null);
	}

	@Override
	public void info(String message) {
		add(Level.INFO, message, null);
	}

	@Override
	public void warn(String message) {
		add(Level.WARN, message, null);
	}

	@Override
	public void warn(String message, Throwable throwable) {
		add(Level.WARN, message, null);
	}

	@Override
	public void error(Throwable throwable) {
		add(Level.ERROR, null, throwable);
	}

	@Override
	public void error(String message, Throwable throwable) {
		add(Level.ERROR, message, throwable);
	}

	private void add(Level level, String message, Throwable throwable) {
		logLevels.add(level);
		logMessages.add(message);
		logThrowables.add(throwable);
	}

	/** Writes the logs to the given log4j logger. */
	public void logTo(Logger logger) {
		for (int i = 0; i < logLevels.size(); i++) {
			logger.log(logLevels.get(i), logMessages.get(i), logThrowables.get(i));
		}
	}
}
