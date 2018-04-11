/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.util;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Triggers a callback in a regular interval
 */
public class Timer {

	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private ScheduledFuture<?> job = null;

	private final Runnable runnable;
	private final Duration duration;

	/** Constructor. */
	public Timer(Runnable runnable, Duration duration) {
		this.runnable = runnable;
		this.duration = duration;
	}

	public synchronized void start() {
		if (job != null) {
			return;
		}

		job = executor.scheduleAtFixedRate(runnable, duration.toMinutes(), duration.toMinutes(), TimeUnit.MINUTES);
	}

	public synchronized void stop() {
		job.cancel(true);
		job = null;
	}

}
