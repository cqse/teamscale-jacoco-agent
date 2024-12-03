package com.teamscale.jacoco.agent.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

/**
 * An appender that collects log statements in its buffer until it's asked to empty it into a given
 * {@link LoggerContext}. It can be used to log log-messages before an actual logging appender has been configured using
 * the options passed to the agent.
 * */
public class DelayedLogAppender extends AppenderBase<ILoggingEvent> {

	private static final List<ILoggingEvent> BUFFER = new ArrayList<>();

	/** Appends the given {@link ILoggingEvent} to logger's buffer. */
	@Override
	protected void append(ILoggingEvent eventObject) {
		synchronized (BUFFER) {
			BUFFER.add(eventObject);
		}
	}

	/**
	 * logs the log-messages currently stored in the buffer into the given {@link LoggerContext} and empties the
	 * buffer.
	 * */
	public static void logTo(LoggerContext context) {
		synchronized (BUFFER) {
			for (ILoggingEvent event : BUFFER) {
				context.getLogger(event.getLoggerName()).callAppenders(event);
			}
			BUFFER.clear();
		}
	}
}
