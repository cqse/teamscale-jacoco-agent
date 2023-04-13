package com.teamscale.test_impacted.commons;

import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerUtils {

	private final static Logger MAIN_LOGGER;

	static {
		MAIN_LOGGER = Logger.getLogger("com.teamscale");
		MAIN_LOGGER.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter() {
			private static final String format = "[%1$s] %2$s%n";

			@Override
			public synchronized String format(LogRecord lr) {
				return String.format(format,
						lr.getLevel().getLocalizedName(),
						lr.getMessage()
				);
			}
		});
		MAIN_LOGGER.addHandler(handler);
	}

	public static Logger getLogger(Class<?> clazz) {
		return Logger.getLogger(clazz.getName());
	}
}
