package com.teamscale.jacoco.agent.options;

import okhttp3.HttpUrl;

/** Credentials for accessing a Teamscale instance. */
public class TeamscaleCredentials {

	/** The URL of the Teamscale server. */
	public final HttpUrl url;

	/** The user name used to authenticate against Teamscale. */
	public final String userName;

	/** The user's access key. */
	public final String accessKey;

	public TeamscaleCredentials(HttpUrl url, String userName, String userAccessToken) {
		this.url = url;
		this.userName = userName;
		this.accessKey = userAccessToken;
	}
}
