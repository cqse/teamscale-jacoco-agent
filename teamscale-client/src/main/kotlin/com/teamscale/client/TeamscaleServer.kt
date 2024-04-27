package com.teamscale.client

import okhttp3.HttpUrl
import java.net.InetAddress
import java.net.UnknownHostException
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

	/** The user name used to authenticate against Teamscale.  */
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
				return createDefaultMessage()
			}
			return field
		}

	private fun createDefaultMessage(): String {
		// we do not include the IP address here as one host may have
		// - multiple network interfaces
		// - each with multiple IP addresses
		// - in either IPv4 or IPv6 format
		// - and it is unclear which of those is "the right one" or even just which is useful (e.g. loopback or virtual
		// adapters are not useful and might even confuse readers)
		var hostnamePart = "uploaded from "
		hostnamePart += try {
			"hostname: " + InetAddress.getLocalHost().hostName
		} catch (e: UnknownHostException) {
			"an unknown computer"
		}

		var revisionPart = ""
		if (revision != null) {
			revisionPart = "\nfor revision: $revision"
		}

		var configIdPart = ""
		if (configId != null) {
			configIdPart = "\nprofiler configuration ID: $configId"
		}

		return """$partition coverage uploaded at ${DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())}

$hostnamePart$revisionPart$configIdPart"""
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
	fun canConnectToTeamscale(): Boolean {
		return url != null && userName != null && userAccessToken != null
	}

	/** Returns whether all fields are null.  */
	fun hasAllFieldsNull(): Boolean {
		return url == null && project == null && userName == null && userAccessToken == null && partition == null && commit == null && revision == null
	}

	/** Returns whether either a commit or revision has been set.  */
	fun hasCommitOrRevision(): Boolean {
		return commit != null || revision != null
	}

	override fun toString(): String {
		val at = if (revision != null) {
			"revision $revision"
		} else {
			"commit $commit"
		}
		return "Teamscale $url as user $userName for $project to $partition at $at"
	}

	/** Creates a copy of the [TeamscaleServer] configuration, but with the given project and commit set.  */
	fun withProjectAndCommit(teamscaleProject: String?, commitDescriptor: CommitDescriptor?) =
		TeamscaleServer().apply {
			url = this@TeamscaleServer.url
			userName = this@TeamscaleServer.userName
			userAccessToken = this@TeamscaleServer.userAccessToken
			partition = this@TeamscaleServer.partition
			project = teamscaleProject
			commit = commitDescriptor
		}

	/** Creates a copy of the [TeamscaleServer] configuration, but with the given project and revision set.  */
	fun withProjectAndRevision(teamscaleProject: String?, revision: String?): TeamscaleServer {
		val teamscaleServer = TeamscaleServer()
		teamscaleServer.url = url
		teamscaleServer.userName = userName
		teamscaleServer.userAccessToken = userAccessToken
		teamscaleServer.partition = partition
		teamscaleServer.project = teamscaleProject
		teamscaleServer.revision = revision
		return teamscaleServer
	}
}
