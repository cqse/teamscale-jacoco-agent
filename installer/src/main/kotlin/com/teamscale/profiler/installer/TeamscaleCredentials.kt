package com.teamscale.profiler.installer

import okhttp3.HttpUrl

/** Data class for Teamscale credentials.  */
class TeamscaleCredentials(
	/** Teamscale base URL.  */
	val url: HttpUrl?,
	/** Teamscale username.  */
	val username: String?,
	/** Teamscale access key.  */
	val accessKey: String?
)
