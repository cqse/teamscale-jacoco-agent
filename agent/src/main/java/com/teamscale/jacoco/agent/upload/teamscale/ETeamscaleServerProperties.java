package com.teamscale.jacoco.agent.upload.teamscale;

import com.teamscale.client.TeamscaleServer;

/** Describes all the fields of the {@link TeamscaleServer}. */
public enum ETeamscaleServerProperties {

	/** See {@link TeamscaleServer#url} */
	URL,
	/** See {@link TeamscaleServer#project} */
	PROJECT,
	/** See {@link TeamscaleServer#userName} */
	USER_NAME,
	/** See {@link TeamscaleServer#userAccessToken} */
	USER_ACCESS_TOKEN,
	/** See {@link TeamscaleServer#partition} */
	PARTITION,
	/** See {@link TeamscaleServer#commit} */
	COMMIT,
	/** See {@link TeamscaleServer#revision} */
	REVISION,
	/** See {@link TeamscaleServer#getMessage()} */
	MESSAGE;
}
