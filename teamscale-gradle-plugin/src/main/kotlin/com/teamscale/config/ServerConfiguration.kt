package com.teamscale.config

class ServerConfiguration(private val parent: ServerConfiguration?) {

    /** The url of the Teamscale server. */
    var url: String? = null
        get() = field ?: parent?.url

    /** The project id for which artifacts should be uploaded. */
    var project: String? = null
        get() = field ?: parent?.project

    /** The user name of the Teamscale user.   */
    var userName: String? = null
        get() = field ?: parent?.userName

    /** The access token of the user.   */
    var userAccessToken: String? = null
        get() = field ?: parent?.userAccessToken

}
