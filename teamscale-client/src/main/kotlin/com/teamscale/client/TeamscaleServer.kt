package com.teamscale.client

import okhttp3.HttpUrl

/** Holds Teamscale server details.  */
class TeamscaleServer {

    /** The URL of the Teamscale server.  */
    var url: HttpUrl? = null

    /** The project id within Teamscale.  */
    var project: String? = null

    /** The user name used to authenticate against Teamscale.  */
    var userName: String? = null

    /** The user's access token.  */
    var userAccessToken: String? = null

    /** The partition to upload reports to.  */
    var partition: String? = null

    /** The corresponding code commit to which the coverage belongs.  */
    var commit: CommitDescriptor? = null

    /** The commit message shown in the Teamscale UI for the coverage upload.  */
    var message = "Agent coverage upload"

    /** Returns if all required fields are non-null.  */
    fun hasAllRequiredFieldsSet(): Boolean {
        return url != null &&
                project != null &&
                userName != null &&
                userAccessToken != null &&
                partition != null &&
                commit != null
    }

    /** Returns whether all required fields are null.  */
    fun hasAllRequiredFieldsNull(): Boolean {
        return url == null &&
                project == null &&
                userName == null &&
                userAccessToken == null &&
                partition == null &&
                commit == null
    }

    override fun toString(): String {
        return "Teamscale $url as user $userName for $project to $partition at $commit"
    }
}
