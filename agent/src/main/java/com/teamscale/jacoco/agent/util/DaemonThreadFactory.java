package com.teamscale.jacoco.agent.util;

import java.util.concurrent.ThreadFactory;

/**
 * {@link ThreadFactory} that only produces deamon threads (threads that don't prevent JVM shutdown) with a fixed name.
 */
public class DaemonThreadFactory implements ThreadFactory {

	private final String threadName;

	public DaemonThreadFactory(Class<?> owningClass, String threadName) {
		this.threadName = "Teamscale JaCoCo Agent " + owningClass.getSimpleName() + " " + threadName;
	}

	@Override
	public Thread newThread(Runnable runnable) {
		Thread thread = new Thread(runnable, threadName);
		thread.setDaemon(true);
		return thread;
	}
}
