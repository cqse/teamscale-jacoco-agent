package com.teamscale.jacoco.agent.convert;

import com.teamscale.report.util.ILogger;

/** Logger that prints all output to the console. */
class CommandLineLogger implements ILogger {

	@Override
	public void debug(String message) {
		System.out.println(message);
	}

	@Override
	public void info(String message) {
		System.out.println(message);
	}

	@Override
	public void warn(String message) {
		System.err.println(message);
	}

	@Override
	public void warn(String message, Throwable throwable) {
		System.err.println(message);
		throwable.printStackTrace();
	}

	@Override
	public void error(Throwable throwable) {
		throwable.printStackTrace();
	}

	@Override
	public void error(String message, Throwable throwable) {
		System.err.println(message);
		throwable.printStackTrace();
	}
}
