package com.teamscale.client

/**
 * DTO that is sent to the profiler as a response of registering against
 * Teamscale and contains the profiler ID that was assigned to it as well as the
 * configuration it should pick up.
 */
class ProfilerRegistration {
	/** The ID that was assigned to this instance of the profiler.  */
	@JvmField
	var profilerId: String? = null

	/** The profiler configuration to use.  */
	@JvmField
	var profilerConfiguration: ProfilerConfiguration? = null
}
