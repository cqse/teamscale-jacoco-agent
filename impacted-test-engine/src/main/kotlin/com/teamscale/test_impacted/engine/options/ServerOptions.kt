package com.teamscale.test_impacted.engine.options

/** Represents options for the connection to the Teamscale server.  */
class ServerOptions {
	/** The Teamscale url.
	 * @see [Builder.url] */
	var url: String? = null
		private set

	/** The Teamscale project id for which artifacts should be uploaded.
	 * @see [Builder.project] */
	var project: String? = null
		private set

	/** The username of the Teamscale user.
	 * @see [Builder.userName] */
	var userName: String? = null
		private set

	/** The access token of the user.
	 * @see [Builder.userAccessToken] */
	var userAccessToken: String? = null
		private set

	/** The builder for [ServerOptions].  */
	class Builder {
		private val serverOptions = ServerOptions()

		/** @see [ServerOptions.url] */
		fun url(url: String): Builder {
			serverOptions.url = url
			return this
		}

		/** @see [ServerOptions.project] */
		fun project(project: String): Builder {
			serverOptions.project = project
			return this
		}

		/** @see [ServerOptions.userName] */
		fun userName(userName: String): Builder {
			serverOptions.userName = userName
			return this
		}

		/** @see [ServerOptions.userAccessToken] */
		fun userAccessToken(userAccessToken: String): Builder {
			serverOptions.userAccessToken = userAccessToken
			return this
		}

		/** Checks field conditions and returns the built [ServerOptions].  */
		fun build(): ServerOptions {
			check(!serverOptions.url.isNullOrBlank()) { "The server URL must be set." }
			check(!serverOptions.project.isNullOrBlank()) { "The Teamscale project must be set." }
			check(!serverOptions.userName.isNullOrBlank()) { "The user name must be set." }
			check(!serverOptions.userAccessToken.isNullOrBlank()) { "The user access token must be set." }
			return serverOptions
		}
	}

	companion object {
		/** Returns the builder for [ServerOptions].  */
		@JvmStatic
		fun builder() = Builder()
	}
}
