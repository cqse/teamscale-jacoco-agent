package com.teamscale.jacoco.agent.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.teamscale.jacoco.agent.options.TeamscaleCredentials;

public class TeamscaleLogAppender extends AppenderBase<ILoggingEvent> {

	public static void start(TeamscaleCredentials credentials) {
		TeamscaleLogAppender.credentials = credentials;
		if (credentials == null) {
			enabled = false;
			// TODO (FS) cancel any background threads and clear any buffered messages
		}
	}

	private static TeamscaleCredentials credentials = null;
	private static boolean enabled = true;

	@Override
	protected void append(ILoggingEvent eventObject) {
		if (!enabled) {
			return;
		}
		// TODO (FS) buffer and send events to Teamscale asynchronously
		// TODO (FS) log messages may arrive before credentials are set. buffer in this case until either disabled() or credentials are available
		System.err.println(
				"----> event: " + eventObject.getTimeStamp() + " " + eventObject.getLevel() + " " + eventObject.getLoggerName() + " " + eventObject.getFormattedMessage());
	}
}
