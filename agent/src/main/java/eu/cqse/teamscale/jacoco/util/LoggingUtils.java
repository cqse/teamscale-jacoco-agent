/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.util;

import eu.cqse.teamscale.jacoco.agent.Agent;
import eu.cqse.teamscale.jacoco.agent.PreMain;
import eu.cqse.teamscale.report.util.ILogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helps initialize the logging framework properly.
 */
public class LoggingUtils {

	static {
		// since we will be logging from our own shutdown hook, we must disable the
		// log4j one. Otherwise it logs a warning to the console on shutdown. Due to
		// this, we need to manually shutdown the logging engine in our own shutdown
		// hook
		ConfigurationFactory.setConfigurationFactory(new ShutdownHookDisablingConfigurationFactory());
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
		ThreadContext.put("defaultLogDir", logDirectory.toString());

		URL url = Agent.class.getResource("log4j2-default.xml");
		InputStream stream = Agent.class.getResourceAsStream("log4j2-default.xml");
		ConfigurationSource source = new ConfigurationSource(stream, url);
		Configurator.initialize(null, source);

		if (caughtException != null) {
			LogManager.getLogger(LoggingUtils.class)
					.error("Failed to resolve path to the agent JAR. Logging to current working directory {}",
							logDirectory, caughtException);
		}
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

		ConfigurationSource source = new ConfigurationSource(new FileInputStream(loggingConfigFile.toFile()),
				loggingConfigFile.toFile());
		Configurator.initialize(null, source);
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
				logger.error(throwable);
			}

			@Override
			public void error(String message, Throwable throwable) {
				logger.error(message, throwable);
			}
		};
	}

	/** Factory for XML config files that disables the shutdown hook. */
	private static class ShutdownHookDisablingConfigurationFactory extends ConfigurationFactory {

		/** {@inheritDoc} */
		@Override
		protected String[] getSupportedTypes() {
			return XmlConfigurationFactory.SUFFIXES;
		}

		/** {@inheritDoc} */
		@Override
		public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
			return new ShutdownHookDisablingConfiguration(loggerContext, source);
		}

	}

	/** Default XML configuration that forces the shutdown hook to be disabled. */
	public static class ShutdownHookDisablingConfiguration extends XmlConfiguration {

		/** Constructor. */
		public ShutdownHookDisablingConfiguration(LoggerContext loggerContext, ConfigurationSource configSource) {
			super(loggerContext, configSource);
		}

		/** {@inheritDoc} */
		@Override
		public boolean isShutdownHookEnabled() {
			return false;
		}

	}

}
