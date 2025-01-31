package com.teamscale.test_impacted.engine.options

/**
 * Represents the configuration options for connecting to a Teamscale server.
 *
 * This includes the server's URL, the project identifier, and the credentials needed for authentication.
 * The class ensures all required fields are set and non-blank during initialization.
 *
 * @property url The URL of the Teamscale server.
 * @property project The identifier of the Teamscale project where artifacts will be uploaded.
 * @property userName The username for authenticating with the Teamscale server.
 * @property userAccessToken The access token for the specified username.
 */
data class ServerOptions(
	val url: String,
	val project: String,
	val userName: String,
	val userAccessToken: String
) {
	init {
		require(url.isNotBlank()) { "The server URL must be set." }
		require(project.isNotBlank()) { "The Teamscale project must be set." }
		require(userName.isNotBlank()) { "The user name must be set." }
		require(userAccessToken.isNotBlank()) { "The user access token must be set." }
	}
}

