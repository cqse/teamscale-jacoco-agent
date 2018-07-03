package eu.cqse.teamscale.jacoco.agent.store.upload.teamscale;

import okhttp3.HttpUrl;

/** Holds Teamscale server details. */
public class TeamscaleServer {

    /** The URL of the Teamscale server. */
    public HttpUrl url;

    /** The project id within Teamscale. */
    public String project;

    /** The user name used to authenticate against Teamscale. */
    public String userName;

    /** The user's access token. */
    public String userAccessToken;

    /** The partition to upload reports to. */
    public String partition;

    /** The corresponding code commit to which the coverage belongs. */
    public CommitDescriptor commit;

    /** The commit message shown in the Teamscale UI for the coverage upload. */
    public String message = "Agent coverage upload";

    /** Returns if all required fields are non-null. */
    public boolean hasAllRequiredFieldsSet() {
        return url != null &&
                project != null &&
                userName != null &&
                userAccessToken != null &&
                partition != null &&
                commit != null;
    }

    /** Returns whether all required fields are null. */
    public boolean hasAllRequiredFieldsNull() {
        return url == null &&
                project == null &&
                userName == null &&
                userAccessToken == null &&
                partition == null &&
                commit == null;
    }

    @Override
    public String toString() {
        return "Teamscale " + url + " as user " + userName + " for " + project + " to " + partition + " at " + commit;
    }
}
