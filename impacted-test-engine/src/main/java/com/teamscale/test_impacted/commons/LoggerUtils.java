package com.teamscale.test_impacted.commons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Provides access to a JUL Logger which is configured to print to the console in a not too noisy format as this appears
 * in the console when executing tests.
 */
public class LoggerUtils {

	private static final Logger MAIN_LOGGER;
	public static final String JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY = "java.util.logging.config.file";

	static {
		// Needs to be at the very top so it also takes affect when setting the log level for Console handlers
		useDefaultJULConfigFile();

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
	 * Normally, the java util logging framework picks up the config file specified via the system property
	 * {@value #JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY}. For some reason, this does not work here, so we need to
	 * teach the log manager to use it.
	 */
	private static void useDefaultJULConfigFile() {
		String loggingPropertiesFilePathString = System.getProperty(JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY);
		if (loggingPropertiesFilePathString == null) {
			return;
		}

		Logger logger = Logger.getLogger(LoggerUtils.class.getName());
		try {
			Path loggingPropertiesFilePath = Paths.get(loggingPropertiesFilePathString);
			if (!loggingPropertiesFilePath.toFile().exists()) {
				logger.warning(
						"Cannot find the file specified via " + JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY + ": " + loggingPropertiesFilePathString);
				return;
			}
			LogManager.getLogManager().readConfiguration(Files.newInputStream(loggingPropertiesFilePath));
		} catch (IOException e) {
			logger.warning(
					"Cannot load the file specified via " + JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY + ": " + loggingPropertiesFilePathString + ". " + e.getMessage());
		}
	}

	/**
	 * Returns a logger for the given class.
	 */
	public static Logger getLogger(Class<?> clazz) {
		return Logger.getLogger(clazz.getName());
	}
}
