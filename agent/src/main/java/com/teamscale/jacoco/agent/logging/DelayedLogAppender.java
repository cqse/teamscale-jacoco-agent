package com.teamscale.jacoco.agent.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * An appender that collects log statements in its buffer until it's asked to empty it into a given
 * {@link LoggerContext}. It can be used to log log-messages before an actual logging appender has been configured using
 * the options passed to the agent.
 *
 * Logback picks this appender up as the default appender because it searches for a default in logback.xml, which in this case
 * references the {@link DelayedLogAppender}, as described <a href="https://logback.qos.ch/manual/configuration.html#auto_configuration">here</a>.
 */
public class DelayedLogAppender extends AppenderBase<ILoggingEvent> {

	private static final CloseableBuffer<ILoggingEvent> BUFFER = new CloseableBuffer<>();

	/** Appends the given {@link ILoggingEvent} to logger's buffer. */
	@Override
	protected void append(ILoggingEvent eventObject) {
		synchronized (BUFFER) {
			if (!BUFFER.append(eventObject)) {
				System.err.println("Attempted to log to a closed delayed log buffer: " + eventObject);
			}
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

	/** Close the Buffer of the DelayedLogAppender. */
	public static void close() {
		synchronized (BUFFER) {
			BUFFER.close();
		}
	}

}
