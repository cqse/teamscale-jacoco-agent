package com.teamscale.client

/** Configuration options for a profiler.  */
class ProfilerConfiguration {
	/** The ID if this configuration.  */
	@JvmField
	var configurationId: String? = null

	/** The options that should be applied to the profiler.  */
	@JvmField
	var configurationOptions: String? = null
}
