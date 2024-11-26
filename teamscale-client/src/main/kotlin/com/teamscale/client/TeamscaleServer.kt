package com.teamscale.client

import okhttp3.HttpUrl
import java.net.InetAddress
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Holds Teamscale server details.  */
class TeamscaleServer {
	/** The URL of the Teamscale server.  */
	@JvmField
	var url: HttpUrl? = null

	/** The project id within Teamscale.  */
	@JvmField
	var project: String? = null

	/** The username used to authenticate against Teamscale.  */
	@JvmField
	var userName: String? = null

	/** The user's access token.  */
	@JvmField
	var userAccessToken: String? = null

	/** The partition to upload reports to.  */
	@JvmField
	var partition: String? = null

	/**
	 * The corresponding code commit to which the coverage belongs. If this is null, the Agent is supposed to
	 * auto-detect the commit from the profiled code.
	 */
	@JvmField
	var commit: CommitDescriptor? = null

	/**
	 * The corresponding code revision to which the coverage belongs. This is currently only supported for testwise
	 * mode.
	 */
	@JvmField
	var revision: String? = null

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	@JvmField
	var repository: String? = null

	/**
	 * The configuration ID that was used to retrieve the profiler configuration. This is only set here to append it to
	 * the default upload message.
	 */
	@JvmField
	var configId: String? = null

	var message: String? = null
		/**
		 * The commit message shown in the Teamscale UI for the coverage upload. If the message is null, auto-generates a
		 * sensible message.
		 */
		get() {
			if (field == null) {
				return buildDefaultMessage()
			}
			return field
		}

	/**
	 * We do not include the IP address here as one host may have
	 * - multiple network interfaces
	 * - each with multiple IP addresses
	 * - in either IPv4 or IPv6 format
	 * - and it is unclear which of those is "the right one" or even just which is useful (e.g. loopback or virtual
	 * adapters are not useful and might even confuse readers)
	 */
	private fun buildDefaultMessage() =
		buildString {
			append("$partition coverage uploaded at ")
			append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()))
			append("\n\nuploaded from ")

			val hostname = runCatching {
				"hostname: " + InetAddress.getLocalHost().hostName
			}.getOrElse {
				"an unknown computer"
			}
			append(hostname)

			if (revision != null) {
				append("\nfor revision: $revision")
			}

			if (configId != null) {
				append("\nprofiler configuration ID: $configId")
			}
		}

	val isConfiguredForSingleProjectTeamscaleUpload: Boolean
		/** Checks if all fields required for a single-project Teamscale upload are non-null.  */
		get() = isConfiguredForServerConnection && partition != null && project != null

	val isConfiguredForMultiProjectUpload: Boolean
		/** Checks if all fields required for a Teamscale upload are non-null, except the project which must be null.  */
		get() = isConfiguredForServerConnection && partition != null && project == null

	val isConfiguredForServerConnection: Boolean
		/** Checks if all required fields to access a Teamscale server are non-null.  */
		get() = url != null && userName != null && userAccessToken != null

	/** Whether a URL, user and access token were provided.  */
	fun canConnectToTeamscale() =
		url != null && userName != null && userAccessToken != null

	/** Returns whether all fields are null.  */
	fun hasAllFieldsNull() =
		url == null
				&& project == null
				&& userName == null
				&& userAccessToken == null
				&& partition == null
				&& commit == null
				&& revision == null

	/** Returns whether either a commit or revision has been set.  */
	fun hasCommitOrRevision() =
		commit != null || revision != null

	/** Checks if another TeamscaleServer has the same project and revision/commit as this TeamscaleServer instance.  */
	fun hasSameProjectAndCommit(other: TeamscaleServer): Boolean {
		if (project != other.project) {
			return false
		}
		if (revision != null) {
			return revision == other.revision
		}
		return commit == other.commit
	}

	override fun toString() =
		buildString {
			append("Teamscale $url as user $userName for $project to $partition at ")
			if (revision != null) {
				append("revision $revision")
				if (repository != null) {
					append(" in repository $repository")
				}
			} else {
				append("commit $commit")
			}
		}

	/** Creates a copy of the [TeamscaleServer] configuration, but with the given project and commit set.  */
	fun withProjectAndCommit(teamscaleProject: String, commitDescriptor: CommitDescriptor): TeamscaleServer {
		val teamscaleServer = TeamscaleServer()
		teamscaleServer.url = url
		teamscaleServer.userName = userName
		teamscaleServer.userAccessToken = userAccessToken
		teamscaleServer.partition = partition
		teamscaleServer.project = teamscaleProject
		teamscaleServer.commit = commitDescriptor
		return teamscaleServer
	}

	/** Creates a copy of the [TeamscaleServer] configuration, but with the given project and revision set.  */
	fun withProjectAndRevision(project: String, revision: String): TeamscaleServer {
		val server = TeamscaleServer()
		server.url = url
		server.userName = userName
		server.userAccessToken = userAccessToken
		server.partition = partition
		server.project = project
		server.revision = revision
		return server
	}
}
