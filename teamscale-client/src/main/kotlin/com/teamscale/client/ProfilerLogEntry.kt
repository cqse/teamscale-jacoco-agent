package com.teamscale.client

/** A log entry to be sent to Teamscale */
class ProfilerLogEntry(
	/** The time of the event */
	var timestamp: Long,

	/** Log message */
	var message: String,

	/** Details, for example, the stack trace */
	var details: String,

	/** Event severity */
	var severity: String
)