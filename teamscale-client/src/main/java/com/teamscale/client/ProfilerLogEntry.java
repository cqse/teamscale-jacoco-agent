package com.teamscale.client;

/** A log entry to be sent to Teamscale */
public class ProfilerLogEntry {

	/** The time of the event */
	private final long timestamp;

	/** Log message */
	private final String message;

	/** Event severity */
	private final String severity;

	/** Details, for example, the stack trace */
	private final String details;

	public ProfilerLogEntry(long timestamp, String message, String details, String severity) {
		this.timestamp = timestamp;
		this.message = message;
		this.severity = severity;
		this.details = details;
	}

}
