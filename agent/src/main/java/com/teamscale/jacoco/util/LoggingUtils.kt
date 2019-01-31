/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.util

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import com.teamscale.jacoco.agent.Agent
import com.teamscale.report.util.ILogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

/**
 * Helps initialize the logging framework properly.
 */
object LoggingUtils {

    private val loggerContext: LoggerContext
        get() = LoggerFactory.getILoggerFactory() as LoggerContext

    /** Returns a logger for the given object's class.  */
    fun getLogger(`object`: Any): Logger {
        return LoggerFactory.getLogger(`object`.javaClass)
    }

    /** Returns a logger for the given class.  */
    fun getLogger(`object`: Class<*>): Logger {
        return LoggerFactory.getLogger(`object`)
    }

    /** Class to use with try-with-resources to close the logging framework's resources.  */
    class LoggingResources : Closeable {

        override fun close() {
            loggerContext.stop()
        }
    }

    /** Initializes the logging to the default configured in the Jar.  */
    fun initializeDefaultLogging(): LoggingResources {
        val stream = Agent::class.java.getResourceAsStream("logback-default.xml")
        reconfigureLoggerContext(stream)
        return LoggingResources()
    }

    /**
     * Reconfigures the logger context to use the configuration XML from the given input stream.
     * C.f. https://logback.qos.ch/manual/configuration.html
     */
    private fun reconfigureLoggerContext(stream: InputStream) {
        val loggerContext = loggerContext
        try {
            val configurator = JoranConfigurator()
            configurator.context = loggerContext
            loggerContext.reset()
            configurator.doConfigure(stream)
        } catch (je: JoranException) {
            // StatusPrinter will handle this
        }

        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext)
    }

    /**
     * Initializes the logging from the given file. If that is `null`,
     * uses [.initializeDefaultLogging] instead.
     */
    @Throws(IOException::class)
    fun initializeLogging(loggingConfigFile: Path?): LoggingResources {
        if (loggingConfigFile == null) {
            return initializeDefaultLogging()
        }

        reconfigureLoggerContext(FileInputStream(loggingConfigFile.toFile()))
        return LoggingResources()
    }

    /** Wraps the given slf4j logger into an [ILogger].  */
    fun wrap(logger: Logger): ILogger {
        return object : ILogger {
            override fun debug(message: String) {
                logger.debug(message)
            }

            override fun info(message: String) {
                logger.info(message)
            }

            override fun warn(message: String) {
                logger.warn(message)
            }

            override fun warn(message: String, throwable: Throwable) {
                logger.warn(message, throwable)
            }

            override fun error(throwable: Throwable) {
                logger.error(throwable.message, throwable)
            }

            override fun error(message: String, throwable: Throwable) {
                logger.error(message, throwable)
            }
        }
    }

}
