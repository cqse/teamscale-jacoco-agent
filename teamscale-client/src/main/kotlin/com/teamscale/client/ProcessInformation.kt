package com.teamscale.client

/** Information about the process and machine the profiler is attached to.  */
class ProcessInformation(
	/** Hostname of the machine it is running on  */
	val hostname: String,
	/** Profiled PID  */
	val pid: String,
	/** The timestamp at which the process was started.  */
	val startedAtTimestamp: Long
)
