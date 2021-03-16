package com.teamscale.client;

import okhttp3.HttpUrl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

		return partition + " coverage uploaded at " +
				DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "\n\n" +
				hostnamePart + revisionPart;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	/** Returns if all required fields are non-null. */
	public boolean hasAllRequiredFieldsSet() {
		return project != null &&
				hasAllRequiredFieldsSetExceptProject();
	}

	/** Returns if all required fields are non-null. */
	public boolean hasAllRequiredFieldsSetExceptProject() {
		return url != null &&
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
