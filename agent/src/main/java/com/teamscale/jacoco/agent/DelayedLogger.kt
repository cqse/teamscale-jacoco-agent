package com.teamscale.jacoco.agent

import com.teamscale.report.util.ILogger
import org.slf4j.Logger
import java.util.*

/**
 * A logger that buffers logs in memory and writes them to the actual logger at a later point.
 * This is needed when stuff needs to be logged before the actual logging framework is initialized.
 */
class DelayedLogger : ILogger {

    /** List of log actions that will be executed once the logger is initialized.  */
    private val logActions = ArrayList<(Logger) -> Unit>()

    override fun debug(message: String) {
        logActions.add { logger -> logger.debug(message) }
    }

    override fun info(message: String) {
        logActions.add { logger -> logger.info(message) }
    }

    override fun warn(message: String) {
        logActions.add { logger -> logger.warn(message) }
    }

    override fun warn(message: String, throwable: Throwable) {
        logActions.add { logger -> logger.warn(message, throwable) }
    }

    override fun error(throwable: Throwable) {
        logActions.add { logger -> logger.error(throwable.message, throwable) }
    }

    override fun error(message: String, throwable: Throwable) {
        logActions.add { logger -> logger.error(message, throwable) }
    }

    /** Writes the logs to the given slf4j logger.  */
    fun logTo(logger: Logger) {
        logActions.forEach { action -> action.invoke(logger) }
    }
}

