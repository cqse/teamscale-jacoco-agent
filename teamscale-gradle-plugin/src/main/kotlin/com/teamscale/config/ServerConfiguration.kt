package com.teamscale.config

import org.gradle.api.GradleException
import java.io.Serializable

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
        if (url.isNullOrBlank()) {
            throw GradleException("Teamscale server url must not be empty!")
        }
        if (project.isNullOrBlank()) {
            throw GradleException("Teamscale project name must not be empty!")
        }
        if (userName.isNullOrBlank()) {
            throw GradleException("Teamscale user name must not be empty!")
        }
        if (userAccessToken.isNullOrBlank()) {
            throw GradleException("Teamscale user access token must not be empty!")
        }
    }
}
