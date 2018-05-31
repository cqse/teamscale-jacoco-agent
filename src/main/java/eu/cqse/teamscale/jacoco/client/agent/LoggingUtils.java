/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.agent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;

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
		URL url = Agent.class.getResource("log4j.xml");
		InputStream stream = Agent.class.getResourceAsStream("log4j.xml");
		ConfigurationSource source = new ConfigurationSource(stream, url);
		Configurator.initialize(null, source);
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
