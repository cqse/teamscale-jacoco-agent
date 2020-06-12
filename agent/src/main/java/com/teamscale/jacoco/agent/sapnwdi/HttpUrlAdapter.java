package com.teamscale.jacoco.agent.sapnwdi;

import com.squareup.moshi.FromJson;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import okhttp3.HttpUrl;

/** Adapter class to parse URLs from NWDI config the same way as in other options. */
public class HttpUrlAdapter {

	@FromJson
	public static HttpUrl parseUrl(String url) {
		return AgentOptionsParser.parseUrl(url);
	}
}
