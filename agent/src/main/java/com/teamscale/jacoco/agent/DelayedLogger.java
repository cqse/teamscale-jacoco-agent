package com.teamscale.jacoco.agent;

import com.teamscale.report.util.ILogger;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A logger that buffers logs in memory and writes them to the actual logger at a later point.
 * This is needed when stuff needs to be logged before the actual logging framework is initialized.
 */
public class DelayedLogger implements ILogger {

	/** List of log actions that will be executed once the logger is initialized. */
	private final List<ILoggerAction> logActions = new ArrayList<>();

	@Override
	public void debug(String message) {
		logActions.add(logger -> logger.debug(message));
	}

	@Override
	public void info(String message) {
		logActions.add(logger -> logger.info(message));
	}

	@Override
	public void warn(String message) {
		logActions.add(logger -> logger.warn(message));
	}

	@Override
	public void warn(String message, Throwable throwable) {
		logActions.add(logger -> logger.warn(message, throwable));
	}

	@Override
	public void error(Throwable throwable) {
		logActions.add(logger -> logger.error(throwable.getMessage(), throwable));
	}

	@Override
	public void error(String message, Throwable throwable) {
		logActions.add(logger -> logger.error(message, throwable));
	}

	/** Writes the logs to the given slf4j logger. */
	public void logTo(Logger logger) {
		logActions.forEach(action -> action.log(logger));
	}

	/** An action to be executed on a logger. */
	private interface ILoggerAction {

		/** Executes the action on the given logger. */
		void log(Logger logger);

	}
}

