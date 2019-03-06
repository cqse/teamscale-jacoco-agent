package com.teamscale.test_impacted.engine.options;

import org.junit.platform.commons.util.Preconditions;

/** Represents options for the connection to the Teamscale server. */
public class ServerOptions {

	/** The Teamscale url. */
	private String url;

	/** The Teamscale project id for which artifacts should be uploaded. */
	private String project;

	/** The user name of the Teamscale user. */
	private String userName;

	/** The access token of the user. */
	private String userAccessToken;

	/** @see #url */
	public String getUrl() {
		return url;
	}

	/** @see #project */
	public String getProject() {
		return project;
	}

	/** @see #userName */
	public String getUserName() {
		return userName;
	}

	/** @see #userAccessToken */
	public String getUserAccessToken() {
		return userAccessToken;
	}

	/** Returns the builder for {@link ServerOptions}. */
	public static Builder builder() {
		return new Builder();
	}

	/** The builder for {@link ServerOptions}. */
	public static class Builder {

		private ServerOptions serverOptions = new ServerOptions();

		private Builder() {
		}

		/** @see #url */
		public Builder url(String url) {
			serverOptions.url = url;
			return this;
		}

		/** @see #project */
		public Builder project(String project) {
			serverOptions.project = project;
			return this;
		}

		/** @see #userName */
		public Builder userName(String userName) {
			serverOptions.userName = userName;
			return this;
		}

		/** @see #userAccessToken */
		public Builder userAccessToken(String userAccessToken) {
			serverOptions.userAccessToken = userAccessToken;
			return this;
		}

		/** Checks field conditions and returns the built {@link ServerOptions}. */
		public ServerOptions build() {
			Preconditions.notBlank(serverOptions.url, "The server URL must be set.");
			Preconditions.notBlank(serverOptions.project, "The Teamscale project must be set.");
			Preconditions.notBlank(serverOptions.userName, "The user name must be set.");
			Preconditions.notBlank(serverOptions.userAccessToken, "The user access token must be set.");
			return serverOptions;
		}


	}
}
