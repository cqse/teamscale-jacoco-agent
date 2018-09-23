/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import eu.cqse.teamscale.jacoco.agent.Agent;
import eu.cqse.teamscale.report.util.ILogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Helps initialize the logging framework properly.
 */
public class LoggingUtils {

	/** Returns a logger for the given object or class. */
	public static Logger getLogger(Object object) {
		if (object instanceof Class) {
			return LoggerFactory.getLogger((Class<?>) object);
		}
		return LoggerFactory.getLogger(object.getClass());
	}

	/** Initializes the logging to the default configured in the Jar. */
	public static void initializeDefaultLogging() {
		InputStream stream = Agent.class.getResourceAsStream("logback-default.xml");
		reconfigureLoggerContext(stream);
	}

	/** Releases all resources associated with the logging framework. Must be called from a shutdown hook. */
	public static void shutDownLogging() {
		getLoggerContext().stop();
	}

	private static LoggerContext getLoggerContext() {
		return (LoggerContext) LoggerFactory.getILoggerFactory();
	}

	/**
	 * Reconfigures the logger context to use the configuration XML from the given input stream.
	 * C.f. https://logback.qos.ch/manual/configuration.html
	 */
	private static void reconfigureLoggerContext(InputStream stream) {
		LoggerContext loggerContext = getLoggerContext();
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(loggerContext);
			loggerContext.reset();
			configurator.doConfigure(stream);
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
	}

	/**
	 * Initializes the logging from the given file. If that is <code>null</code>,
	 * uses {@link #initializeDefaultLogging()} instead.
	 */
	public static void initializeLogging(Path loggingConfigFile) throws IOException {
		if (loggingConfigFile == null) {
			initializeDefaultLogging();
			return;
		}

		reconfigureLoggerContext(new FileInputStream(loggingConfigFile.toFile()));
	}

	/** Wraps the given log4j logger into an {@link ILogger}. */
	public static ILogger wrap(Logger logger) {
		return new ILogger() {
			@Override
			public void debug(String message) {
				logger.debug(message);
			}

			@Override
			public void warn(String message) {
				logger.warn(message);
			}

			@Override
			public void warn(String message, Throwable throwable) {
				logger.warn(message, throwable);
			}

			@Override
			public void error(Throwable throwable) {
				logger.error(throwable.getMessage(), throwable);
			}

			@Override
			public void error(String message, Throwable throwable) {
				logger.error(message, throwable);
			}
		};
	}

}
