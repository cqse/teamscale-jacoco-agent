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
import eu.cqse.teamscale.jacoco.agent.PreMain;
import eu.cqse.teamscale.report.util.ILogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		URISyntaxException caughtException = null;
		Path logDirectory;
		try {
			URI jarFileUri = PreMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			// we assume that the dist zip is extracted and the agent jar not moved
			// Then the log dir should be next to the bin/ dir
			logDirectory = Paths.get(jarFileUri).getParent().getParent().resolve("logs").toAbsolutePath();
		} catch (URISyntaxException e) {
			// we can't log the exception yet since logging is not yet initialized
			caughtException = e;
			// fall back to the working directory
			logDirectory = Paths.get(".").toAbsolutePath();
		}

		// pass the path to the log directory to the logging config XML
		getLoggerContext().putProperty("defaultLogDir", logDirectory.toString());

		InputStream stream = Agent.class.getResourceAsStream("logback-default.xml");
		reconfigureLoggerContext(stream);

		if (caughtException != null) {
			LoggerFactory.getLogger(LoggingUtils.class)
					.error("Failed to resolve path to the agent JAR. Logging to current working directory {}",
							logDirectory, caughtException);
		}
	}

	/** Releases all resources associated with the logging framework. Must be called from a shutdown hook. */
	public static void shutDownLogging() {
		getLoggerContext().stop();
	}

	private static LoggerContext getLoggerContext() {
		return (LoggerContext) LoggerFactory.getILoggerFactory();
	}

	/** C.f. https://logback.qos.ch/manual/configuration.html */
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
