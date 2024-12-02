package com.teamscale.jacoco.agent.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

public class DelayedLogAppender extends AppenderBase<ILoggingEvent> {

	private static final List<ILoggingEvent> BUFFER = new ArrayList<>();

	@Override
	protected void append(ILoggingEvent eventObject) {
		synchronized (BUFFER) {
			BUFFER.add(eventObject);
		}
	}

	public static void logTo(LoggerContext context) {
		synchronized (BUFFER) {
			for (ILoggingEvent event : BUFFER) {
				context.getLogger(event.getLoggerName()).callAppenders(event);
			}
			BUFFER.clear();
		}
	}

	public static void addAppenderTo(LoggerContext context) {
		DelayedLogAppender appender = new DelayedLogAppender();
		appender.setContext(context);
		appender.start();

		Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(appender);
	}

}
