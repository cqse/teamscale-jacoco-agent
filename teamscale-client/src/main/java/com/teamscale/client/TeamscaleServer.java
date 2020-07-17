package com.teamscale.client;

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

	/**
	 * The corresponding code commit to which the coverage belongs. If this is null, the Agent is supposed to
	 * auto-detect the commit from the profiled code.
	 */
	public CommitDescriptor commit;

	/**
	 * The corresponding code revision to which the coverage belongs. This is currently only supported for testwise
	 * mode.
	 */
	public String revision;


	/** The commit message shown in the Teamscale UI for the coverage upload. */
	public String message = "Agent coverage upload";

	/** Returns if all required fields are non-null. */
	public boolean hasAllRequiredFieldsSet() {
		return url != null &&
				project != null &&
				userName != null &&
				userAccessToken != null &&
				partition != null;
	}

	/** Returns whether all required fields are null. */
	public boolean hasAllRequiredFieldsNull() {
		return url == null &&
				project == null &&
				userName == null &&
				userAccessToken == null &&
				partition == null;
	}

	/** Returns whether either a commit or revision has been set. */
	public boolean hasCommitOrRevision() {
		return commit != null || revision != null;
	}

	@Override
	public String toString() {
		String at;
		if (revision != null) {
			at = "revision " + revision;
		} else {
			at = "commit " + commit;
		}
		return "Teamscale " + url + " as user " + userName + " for " + project + " to " + partition + " at " + at;
	}

	/** Creates a copy of the {@link TeamscaleServer} configuration, but with the given project and commit set. */
	public TeamscaleServer withProject(String teamscaleProject, CommitDescriptor commitDescriptor) {
		TeamscaleServer tsApplication = new TeamscaleServer();
		tsApplication.url = url;
		tsApplication.userName = userName;
		tsApplication.userAccessToken = userAccessToken;
		tsApplication.partition = partition;
		tsApplication.project = teamscaleProject;
		tsApplication.commit = commitDescriptor;
		return tsApplication;
	}
}
