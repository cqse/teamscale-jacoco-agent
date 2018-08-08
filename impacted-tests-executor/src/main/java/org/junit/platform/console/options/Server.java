package org.junit.platform.console.options;

/** Holds information about the Teamscale server. */
public class Server {

	/** The url of the teamscale server. */
	public String url;

	/** The project id for which artifacts should be uploaded. */
	public String project;

	/** The user name of the Teamscale user. */
	public String userName;

	/** The access token of the user. */
	public String userAccessToken;

}
