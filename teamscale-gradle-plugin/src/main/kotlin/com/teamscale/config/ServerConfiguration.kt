package com.teamscale.config

import java.io.Serializable
import org.gradle.api.GradleException

data class ServerConfiguration(
	/** The url of the Teamscale server. */
	var url: String? = null,
	/** The project id for which artifacts should be uploaded. */
	var project: String? = null,
	/** The username of the Teamscale user.   */
	var userName: String? = null,
	/** The access token of the user.   */
	var userAccessToken: String? = null
) : Serializable {
	fun validate() {
		check(!url.isNullOrBlank()) { throw ConfigException("server url") }
		check(!project.isNullOrBlank()) { throw ConfigException("project name")}
		check(!userName.isNullOrBlank()) { throw ConfigException("user name") }
		check(!userAccessToken.isNullOrBlank()) { throw ConfigException("user access token") }
	}

	class ConfigException(field: String) : GradleException("Teamscale $field must not be empty!")
}
