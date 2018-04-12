/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.util;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Triggers a callback in a regular interval. Note that the spawned threads are
 * Daemon threads, i.e. they will not prevent the JVM from shutting down.
 */
public class Timer {

	/** Runs the job on a background daemon thread. */
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, runnable -> {
		Thread thread = Executors.defaultThreadFactory().newThread(runnable);
		thread.setDaemon(true);
		return thread;
	});

	/** The currently running job or <code>null</code>. */
	private ScheduledFuture<?> job = null;

	/** The job to execute periodically. */
	private final Runnable runnable;

	/** Duration between two job executions. */
	private final Duration duration;

	/** Constructor. */
	public Timer(Runnable runnable, Duration duration) {
		this.runnable = runnable;
		this.duration = duration;
	}

	/** Starts the regular job. */
	public synchronized void start() {
		if (job != null) {
			return;
		}

		job = executor.scheduleAtFixedRate(runnable, duration.toMinutes(), duration.toMinutes(), TimeUnit.MINUTES);
	}

	/**
	 * Waits until this timer is stopped or the scheduled job throws an exception.
	 */
	public void waitUntilTimerIsStopped() {
		if (job == null) {
			return;
		}

		try {
			job.get();
		} catch (InterruptedException | ExecutionException e) {
			// ignore exception
		}
	}

	/** Stops the regular job, possibly aborting it. */
	public synchronized void stop() {
		job.cancel(false);
		job = null;
	}

}
