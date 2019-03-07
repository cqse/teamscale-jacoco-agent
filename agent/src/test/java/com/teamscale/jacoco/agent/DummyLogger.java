package com.teamscale.jacoco.agent;

import com.teamscale.report.util.ILogger;

/** Implements {@link ILogger} as NOP actions. */
public class DummyLogger implements ILogger {
	@Override
	public void debug(String message) {
		// nothing to do here
	}

	@Override
	public void info(String message) {
		// nothing to do here
	}

	@Override
	public void warn(String message) {
		// nothing to do here
	}

	@Override
	public void warn(String message, Throwable throwable) {
		// nothing to do here
	}

	@Override
	public void error(Throwable throwable) {
		// nothing to do here
	}

	@Override
	public void error(String message, Throwable throwable) {
		// nothing to do here
	}
}
