package com.teamscale.config

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
}
