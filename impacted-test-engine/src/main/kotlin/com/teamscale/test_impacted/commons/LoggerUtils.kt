package com.teamscale.test_impacted.commons

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.*

/**
 * Provides access to a JUL Logger which is configured to print to the console in a not too noisy format as this appears
 * in the console when executing tests.
 */
object LoggerUtils {
	private val MAIN_LOGGER = Logger.getLogger("com.teamscale")
	private const val JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY = "java.util.logging.config.file"

	init {
		// Needs to be at the very top so it also takes affect when setting the log level for Console handlers
		useDefaultJULConfigFile()

		MAIN_LOGGER.useParentHandlers = false
		val handler = ConsoleHandler()
		handler.formatter = object : SimpleFormatter() {
			@Synchronized
			override fun format(lr: LogRecord) =
				String.format("[%1\$s] %2\$s%n", lr.level.localizedName, lr.message)
		}
		MAIN_LOGGER.addHandler(handler)
	}

	/**
	 * Normally, the java util logging framework picks up the config file specified via the system property
	 * {@value #JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY}. For some reason, this does not work here, so we need to
	 * teach the log manager to use it.
	 */
	private fun useDefaultJULConfigFile() {
		val loggingPropertiesFilePathString =
			System.getProperty(JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY)
				?: return

		val logger = getLogger(LoggerUtils::class.java)
		try {
			val propertiesFilePath = Paths.get(loggingPropertiesFilePathString)
			if (!propertiesFilePath.toFile().exists()) {
				logger.warning(
					"Cannot find the file specified via $JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY: $loggingPropertiesFilePathString"
				)
				return
			}
			LogManager.getLogManager().readConfiguration(Files.newInputStream(propertiesFilePath))
		} catch (e: IOException) {
			logger.warning(
				"Cannot load the file specified via $JAVA_UTIL_LOGGING_CONFIG_FILE_SYSTEM_PROPERTY: $loggingPropertiesFilePathString. ${e.message}"
			)
		}
	}

	/**
	 * Returns a logger for the given class.
	 */
	@JvmStatic
	fun getLogger(clazz: Class<*>) = Logger.getLogger(clazz.name)
}
