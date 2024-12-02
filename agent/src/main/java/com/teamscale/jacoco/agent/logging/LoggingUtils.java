/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.teamscale.jacoco.agent.Agent;
import com.teamscale.jacoco.agent.util.NullOutputStream;
import com.teamscale.report.util.ILogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Helps initialize the logging framework properly.
 */
public class LoggingUtils {

	/** Returns a logger for the given object's class. */
	public static Logger getLogger(Object object) {
		return LoggerFactory.getLogger(object.getClass());
	}

	/** Returns a logger for the given class. */
	public static Logger getLogger(Class<?> object) {
		return LoggerFactory.getLogger(object);
	}

	/** Class to use with try-with-resources to close the logging framework's resources. */
	public static class LoggingResources implements AutoCloseable {

		@Override
		public void close() {
			getLoggerContext().stop();
		}
	}

	/** Initializes the logging to the default configured in the Jar. */
	public static LoggingResources initializeDefaultLogging() {
		InputStream stream = Agent.class.getResourceAsStream("logback-default.xml");
		reconfigureLoggerContext(stream);
		return new LoggingResources();
	}

	/**
	 * Returns the logger context.
	 */
	public static LoggerContext getLoggerContext() {
		return (LoggerContext) LoggerFactory.getILoggerFactory();
	}

	/**
	 * Extracts the stack trace from an ILoggingEvent using ThrowableProxyUtil.
	 *
	 * @param event the logging event containing the exception
	 * @return the stack trace as a String, or null if no exception is associated
	 */
	public static String getStackTraceFromEvent(ILoggingEvent event) {
		IThrowableProxy throwableProxy = event.getThrowableProxy();

		if (throwableProxy != null) {
			// Use ThrowableProxyUtil to convert the IThrowableProxy to a String
			return ThrowableProxyUtil.asString(throwableProxy);
		}

		return null;
	}

	/**
	 * Reconfigures the logger context to use the configuration XML from the given input stream. Cf. <a
	 * href="https://logback.qos.ch/manual/configuration.html">https://logback.qos.ch/manual/configuration.html</a>
	 */
	private static void reconfigureLoggerContext(InputStream stream) {
		StatusPrinter.setPrintStream(new PrintStream(new NullOutputStream()));
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
	 * Initializes the logging from the given file. If that is <code>null</code>, uses {@link
	 * #initializeDefaultLogging()} instead.
	 */
	public static LoggingResources initializeLogging(Path loggingConfigFile) throws IOException {
		if (loggingConfigFile == null) {
			return initializeDefaultLogging();
		}

		reconfigureLoggerContext(new FileInputStream(loggingConfigFile.toFile()));
		return new LoggingResources();
	}

	/** Initializes debug logging. */
	public static LoggingResources initializeDebugLogging(Path logDirectory) {
		if (logDirectory != null) {
			DebugLogDirectoryPropertyDefiner.filePath = logDirectory;
		}
		InputStream stream = Agent.class.getResourceAsStream("logback-default-debugging.xml");
		reconfigureLoggerContext(stream);
		return new LoggingResources();
	}

	/** Wraps the given slf4j logger into an {@link ILogger}. */
	public static ILogger wrap(Logger logger) {
		return new ILogger() {
			@Override
			public void debug(String message) {
				logger.debug(message);
			}

			@Override
			public void info(String message) {
				logger.info(message);
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
