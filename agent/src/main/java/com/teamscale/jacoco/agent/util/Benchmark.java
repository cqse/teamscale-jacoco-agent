package com.teamscale.jacoco.agent.util;

import org.slf4j.Logger;

/**
 * Measures how long a certain piece of code takes and logs it to the debug log.
 * <p>
 * Use this in a try-with-resources. Time measurement starts when the resource
 * is created and ends when it is closed.
 */
public class Benchmark implements AutoCloseable {

	/** The logger. */
	private final Logger logger = LoggingUtils.getLogger(this);

	/** The time when the resource was created. */
	private final long startTime;

	/** The description to use in the log message. */
	private String description;

	/** Constructor. */
	public Benchmark(String description) {
		this.description = description;
		startTime = System.nanoTime();
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		long endTime = System.nanoTime();
		logger.debug("{} took {}s", description, (endTime - startTime) / 1_000_000_000L);
	}
}
