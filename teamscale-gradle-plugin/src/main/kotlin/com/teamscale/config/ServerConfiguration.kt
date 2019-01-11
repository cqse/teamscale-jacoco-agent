package com.teamscale.config

import org.gradle.api.GradleException
import java.io.Serializable

class ServerConfiguration : Serializable {

    /** The url of the Teamscale server. */
    var url: String? = null

    /** The project id for which artifacts should be uploaded. */
    var project: String? = null

    /** The user name of the Teamscale user.   */
    var userName: String? = null

    /** The access token of the user.   */
    var userAccessToken: String? = null

    override fun toString(): String {
        return "ServerConfiguration(url=$url, project=$project, userName=$userName, userAccessToken=$userAccessToken)"
    }

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
