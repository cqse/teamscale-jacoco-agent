package com.teamscale.client;

public class ProfilerLogEntry {

	private final long timestamp;

	private final String message;

	private final String severity;

	public ProfilerLogEntry(long timestamp, String message, String severity) {
		this.timestamp = timestamp;
		this.message = message;
		this.severity = severity;
	}

}
