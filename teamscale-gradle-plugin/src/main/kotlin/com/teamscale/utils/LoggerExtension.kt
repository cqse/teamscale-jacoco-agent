package com.teamscale.utils

import com.teamscale.report.util.ILogger
import org.gradle.api.logging.Logger

/** Wraps the gradle log4j logger into an ILogger. */
fun Logger.wrapInILogger(): ILogger {
	val logger = this
	return object : ILogger {
		override fun debug(message: String) = logger.debug(message)
		override fun info(message: String) = logger.info(message)
		override fun warn(message: String) = logger.warn(message)
		override fun warn(message: String, throwable: Throwable?) = logger.warn(message, throwable)
		override fun error(throwable: Throwable) = logger.error("", throwable)
		override fun error(message: String, throwable: Throwable?) = logger.error(message, throwable)
	}
}
