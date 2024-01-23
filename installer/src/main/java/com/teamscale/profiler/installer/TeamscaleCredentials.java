package com.teamscale.profiler.installer;

import okhttp3.HttpUrl;

/** Data class for Teamscale credentials. */
public class TeamscaleCredentials {

	/** Teamscale base URL. */
	public final HttpUrl url;

	/** Teamscale username. */
	public final String username;

	/** Teamscale access key. */
	public final String accessKey;

	public TeamscaleCredentials(HttpUrl url, String user, String accessKey) {
		this.url = url;
		this.username = user;
		this.accessKey = accessKey;
	}
}
