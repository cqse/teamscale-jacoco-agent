package com.teamscale.jacoco.util

import org.slf4j.Logger
import java.io.Closeable

/**
 * Measures how long a certain piece of code takes and logs it to the debug log.
 *
 *
 * Use this in a try-with-resources. Time measurement starts when the resource
 * is created and ends when it is closed.
 */
class Benchmark
/** Constructor.  */
    (
    /** The description to use in the log message.  */
    private val description: String
) : Closeable {

    /** The logger.  */
    private val logger = LoggingUtils.getLogger(this)

    /** The time when the resource was created.  */
    private val startTime: Long

    init {
        startTime = System.nanoTime()
    }

    /** {@inheritDoc}  */
    override fun close() {
        val endTime = System.nanoTime()
        logger.debug("{} took {}s", description, (endTime - startTime) / 1_000_000_000L)
    }
}
