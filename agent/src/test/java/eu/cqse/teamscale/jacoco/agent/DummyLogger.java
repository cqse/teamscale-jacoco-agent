package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.report.util.ILogger;

/** Implements {@link ILogger} as NOP actions. */
public class DummyLogger implements ILogger {
	@Override
	public void debug(String message) {

	}

	@Override
	public void info(String message) {

	}

	@Override
	public void warn(String message) {

	}

	@Override
	public void warn(String message, Throwable throwable) {

	}

	@Override
	public void error(Throwable throwable) {

	}

	@Override
	public void error(String message, Throwable throwable) {

	}
}
