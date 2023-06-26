package com.teamscale.profiler.installer;

import okhttp3.HttpUrl;

public class TeamscaleCredentials {

	public final HttpUrl url;

	public final String username;

	public final String accessKey;

	public TeamscaleCredentials(HttpUrl url, String user, String accessKey) {
		this.url = url;
		this.username = user;
		this.accessKey = accessKey;
	}
}
