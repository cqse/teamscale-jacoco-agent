package eu.cqse.teamscale.jacoco.agent.store.upload.teamscale;

import okhttp3.HttpUrl;

/** Holds Teamscale server details. */
public class TeamscaleServer {
    public HttpUrl url;
    public String project;
    public String userName;
    public String userAccessToken;
    public String partition;
    public CommitDescriptor commit;
    public String message = "Agent coverage upload";

    public boolean validate() {
        return url != null &&
                project != null &&
                userName != null &&
                userAccessToken != null &&
                partition != null &&
                commit != null;
    }

    @Override
    public String toString() {
        return "Teamscale " + url + " as user " + userName + " for " + project + " to " + partition + " at " + commit;
    }
}
