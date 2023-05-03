package com.teamscale.test_impacted.commons;

import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Provides access to a JUL Logger which is configured to print to the console in a not too noisy format as this appears
 * in the console when executing tests.
 */
public class LoggerUtils {

	private static final Logger MAIN_LOGGER;

	static {
		MAIN_LOGGER = Logger.getLogger("com.teamscale");
		MAIN_LOGGER.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter() {

			@Override
			public synchronized String format(LogRecord lr) {
				return String.format("[%1$s] %2$s%n", lr.getLevel().getLocalizedName(), lr.getMessage());
			}
		});
		MAIN_LOGGER.addHandler(handler);
	}

	/**
	 * Returns a logger for the given class.
	 */
	public static Logger getLogger(Class<?> clazz) {
		return Logger.getLogger(clazz.getName());
	}
}
