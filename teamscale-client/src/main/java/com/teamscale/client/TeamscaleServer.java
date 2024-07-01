package com.teamscale.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	public String repository;

	/**
	 * The configuration ID that was used to retrieve the profiler configuration. This is only set here to append it to
	 * the default upload message.
	 */
	public String configId;

	private String message = null;

	/**
	 * The commit message shown in the Teamscale UI for the coverage upload. If the message is null, auto-generates a
	 * sensible message.
	 */
	public String getMessage() {
		if (message == null) {
			return createDefaultMessage();
		}
		return message;
	}

	private String createDefaultMessage() {
		// we do not include the IP address here as one host may have
		// - multiple network interfaces
		// - each with multiple IP addresses
		// - in either IPv4 or IPv6 format
		// - and it is unclear which of those is "the right one" or even just which is useful (e.g. loopback or virtual
		// adapters are not useful and might even confuse readers)
		String hostnamePart = "uploaded from ";
		try {
			hostnamePart += "hostname: " + InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostnamePart += "an unknown computer";
		}

		String revisionPart = "";
		if (revision != null) {
			revisionPart = "\nfor revision: " + revision;
		}

		String configIdPart = "";
		if (configId != null) {
			configIdPart = "\nprofiler configuration ID: " + configId;
		}

		return partition + " coverage uploaded at " +
				DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "\n\n" +
				hostnamePart + revisionPart + configIdPart;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	/** Checks if all fields required for a single-project Teamscale upload are non-null. */
	public boolean isConfiguredForSingleProjectTeamscaleUpload() {
		return isConfiguredForServerConnection() && partition != null && project != null;
	}

	/** Checks if all fields required for a Teamscale upload are non-null, except the project which must be null. */
	public boolean isConfiguredForMultiProjectUpload() {
		return isConfiguredForServerConnection() && partition != null && project == null;
	}

	/** Checks if all required fields to access a Teamscale server are non-null. */
	public boolean isConfiguredForServerConnection() {
		return url != null &&
				userName != null &&
				userAccessToken != null;
	}

	/** Whether a URL, user and access token were provided. */
	public boolean canConnectToTeamscale() {
		return url != null && userName != null && userAccessToken != null;
	}

	/** Returns whether all fields are null. */
	public boolean hasAllFieldsNull() {
		return url == null &&
				project == null &&
				userName == null &&
				userAccessToken == null &&
				partition == null &&
				commit == null &&
				revision == null;
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
			if (repository != null) {
				at += "in repository " + repository;
			}
		} else {
			at = "commit " + commit;
		}
		return "Teamscale " + url + " as user " + userName + " for " + project + " to " + partition + " at " + at;
	}

	/** Creates a copy of the {@link TeamscaleServer} configuration, but with the given project and commit set. */
	public TeamscaleServer withProjectAndCommit(String teamscaleProject, CommitDescriptor commitDescriptor) {
		TeamscaleServer teamscaleServer = new TeamscaleServer();
		teamscaleServer.url = url;
		teamscaleServer.userName = userName;
		teamscaleServer.userAccessToken = userAccessToken;
		teamscaleServer.partition = partition;
		teamscaleServer.project = teamscaleProject;
		teamscaleServer.commit = commitDescriptor;
		return teamscaleServer;
	}

	/** Creates a copy of the {@link TeamscaleServer} configuration, but with the given project and revision set. */
	public TeamscaleServer withProjectAndRevision(String teamscaleProject, String revision) {
		TeamscaleServer teamscaleServer = new TeamscaleServer();
		teamscaleServer.url = url;
		teamscaleServer.userName = userName;
		teamscaleServer.userAccessToken = userAccessToken;
		teamscaleServer.partition = partition;
		teamscaleServer.project = teamscaleProject;
		teamscaleServer.revision = revision;
		return teamscaleServer;
	}
}
