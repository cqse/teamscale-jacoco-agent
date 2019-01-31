/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.util

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Triggers a callback in a regular interval. Note that the spawned threads are
 * Daemon threads, i.e. they will not prevent the JVM from shutting down.
 *
 *
 * The timer will abort if the given [.runnable] ever throws an exception.
 */
class Timer
/** Constructor.  */
    (
    /** The job to execute periodically.  */
    private val runnable: Runnable,
    /** Duration between two job executions.  */
    private val durationInMinutes: Long
) {

    /** Runs the job on a background daemon thread.  */
    private val executor = ScheduledThreadPoolExecutor(1) { runnable ->
        val thread = Executors.defaultThreadFactory().newThread(runnable)
        thread.isDaemon = true
        thread
    }

    /** The currently running job or `null`.  */
    private var job: ScheduledFuture<*>? = null

    /** Starts the regular job.  */
    @Synchronized
    fun start() {
        if (job != null) {
            return
        }

        job = executor.scheduleAtFixedRate(runnable, durationInMinutes, durationInMinutes, TimeUnit.MINUTES)
    }

    /** Stops the regular job, possibly aborting it.  */
    @Synchronized
    fun stop() {
        job!!.cancel(false)
        job = null
    }

}
