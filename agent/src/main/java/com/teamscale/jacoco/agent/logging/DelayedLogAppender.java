package com.teamscale.jacoco.agent.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.FilterReply;

/**
 * An appender that collects log statements in its buffer until it's asked to empty it into a given
 * {@link LoggerContext}. It can be used to log log-messages before an actual logging appender has been configured using
 * the options passed to the agent.
 */
public class DelayedLogAppender extends AppenderBase<ILoggingEvent> {

	private static final CloseableBuffer<ILoggingEvent> BUFFER = new CloseableBuffer<>();


	/** Override this to make sure every log message is passed to real logger, which does the filtering on its own. */
	@Override
	public FilterReply getFilterChainDecision(ILoggingEvent event) {
		return FilterReply.ACCEPT;
	}

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
				Logger logger = context.getLogger(event.getLoggerName());
				String formattedMessage = event.getFormattedMessage();
				switch (event.getLevel().levelStr) {
					case "ERROR":
						logger.error(formattedMessage);
						break;
					case "WARN":
						logger.warn(formattedMessage);
						break;
					case "INFO":
						logger.info(formattedMessage);
						break;
					case "DEBUG":
						logger.debug(formattedMessage);
						break;
					case "TRACE":
						logger.trace(formattedMessage);
						break;
					default:
						break;
				}
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

	/** Add the {@link DelayedLogAppender} to the given {@link LoggerContext}. */
	public static void addDelayedAppenderTo(LoggerContext context) {
		DelayedLogAppender logToTeamscaleAppender = new DelayedLogAppender();
		logToTeamscaleAppender.setContext(context);
		logToTeamscaleAppender.start();

		Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(logToTeamscaleAppender);
	}
}
