package com.teamscale.jacoco.agent

import com.teamscale.report.util.ILogger

/** Implements [ILogger] as NOP actions.  */
class DummyLogger : ILogger {
    override fun debug(message: String) {
        // nothing to do here
    }

    override fun info(message: String) {
        // nothing to do here
    }

    override fun warn(message: String) {
        // nothing to do here
    }

    override fun warn(message: String, throwable: Throwable) {
        // nothing to do here
    }

    override fun error(throwable: Throwable) {
        // nothing to do here
    }

    override fun error(message: String, throwable: Throwable) {
        // nothing to do here
    }
}
