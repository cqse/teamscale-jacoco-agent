package com.teamscale.client

import java.io.Serializable
//import org.gradle.api.GradleException

data class ServerConfiguration(
	/** The url of the Teamscale server. */
	var url: String,
	/** The project id for which artifacts should be uploaded. */
	var project: String,
	/** The username of the Teamscale user.   */
	var userName: String,
	/** The access token of the user.   */
	var userAccessToken: String
) : Serializable {
	 fun validate() {
//		 check(url.isNotBlank()) { throw ConfigException("server url") }
//		 check(project.isNotBlank()) { throw ConfigException("project name")}
//		 check(userName.isNotBlank()) { throw ConfigException("user name") }
//		 check(userAccessToken.isNotBlank()) { throw ConfigException("user access token") }
	 }
//
//	class ConfigException(field: String) : GradleException("Teamscale $field must not be empty!")
}
