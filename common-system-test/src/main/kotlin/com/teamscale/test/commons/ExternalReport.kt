package com.teamscale.test.commons

/** Holds a single report that was uploaded to our fake Teamscale server.  */
class ExternalReport(
	@JvmField val reportString: String,
	@JvmField val partition: String?
)
