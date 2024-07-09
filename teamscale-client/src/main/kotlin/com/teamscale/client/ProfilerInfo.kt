package com.teamscale.client

/** Information about the profiler including the process it is attached to as well as the configuration it is running with.  */
class ProfilerInfo(
	/** Information about the machine and process the profiler is running on.  */
	var processInformation: ProcessInformation,
	/** Concrete config that the profiler is running with.  */
	@JvmField var profilerConfiguration: ProfilerConfiguration?
)
