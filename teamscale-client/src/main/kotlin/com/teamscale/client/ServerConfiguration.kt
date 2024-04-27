package com.teamscale.client

import java.io.Serializable

data class ServerConfiguration(
    /** The url of the Teamscale server. */
    var url: String,
    /** The project id for which artifacts should be uploaded. */
    var project: String,
    /** The user name of the Teamscale user.   */
    var userName: String,
    /** The access token of the user.   */
    var userAccessToken: String
) : Serializable
