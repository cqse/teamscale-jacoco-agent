package eu.cqse.config

import java.io.Serializable

data class Server(
        /** The url of the teamscale server.   */
        var url: String? = null,

        /** The project id for which artifacts should be uploaded.   */
        var project: String? = null,

        /** The user name of the Teamscale user.   */
        var userName: String? = null,

        /** The access token of the user.   */
        var userAccessToken: String? = null
) : Serializable {
    fun copyWithDefault(toCopy: Server, default: Server) {
        url = toCopy.url ?: default.url
        userName = toCopy.userName ?: default.userName
        userAccessToken = toCopy.userAccessToken ?: default.userAccessToken
        project = toCopy.project ?: default.project
    }
}
