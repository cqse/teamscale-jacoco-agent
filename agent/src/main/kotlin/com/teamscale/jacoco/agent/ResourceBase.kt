package com.teamscale.jacoco.agent

import com.teamscale.client.CommitDescriptor.Companion.parse
import com.teamscale.client.TeamscaleServer
import com.teamscale.jacoco.agent.logging.LoggingUtils
import com.teamscale.report.testwise.model.RevisionInfo
import org.conqat.lib.commons.string.StringUtils
import org.slf4j.Logger
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * The resource of the Jersey + Jetty http server holding all the endpoints specific for the [AgentBase].
 */
abstract class ResourceBase {
	/** The logger.  */
	@JvmField
	protected val logger: Logger = LoggingUtils.getLogger(this)

	@get:Path("/partition")
	@get:GET
	val partition: String
		/** Returns the partition for the Teamscale upload.  */
		get() = Optional.ofNullable<String>(agentBase.options.teamscaleServerOptions.partition)
			.orElse("")

	@get:Path("/message")
	@get:GET
	val message: String
		/** Returns the upload message for the Teamscale upload.  */
		get() = Optional.ofNullable<String>(agentBase.options.teamscaleServerOptions.message)
			.orElse("")

	@get:Produces(MediaType.APPLICATION_JSON)
	@get:Path("/revision")
	@get:GET
	val revision: RevisionInfo
		/** Returns revision information for the Teamscale upload.  */
		get() = revisionInfo

	@get:Produces(MediaType.APPLICATION_JSON)
	@get:Path("/commit")
	@get:GET
	val commit: RevisionInfo
		/** Returns revision information for the Teamscale upload.  */
		get() = revisionInfo

	/** Handles setting the partition name.  */
	@PUT
	@Path("/partition")
	fun setPartition(partitionString: String): Response? {
		val partition = StringUtils.removeDoubleQuotes(partitionString)
		if (partition == null || partition.isEmpty()) {
			handleBadRequest("The new partition name is missing in the request body! Please add it as plain text.")
		}

		logger.debug("Changing partition name to $partition")
		agentBase.dumpReport()
		agentBase.controller.sessionId = partition
		agentBase.options.teamscaleServerOptions.partition = partition
		return Response.noContent().build()
	}

	/** Handles setting the upload message.  */
	@PUT
	@Path("/message")
	fun setMessage(messageString: String): Response? {
		val message = StringUtils.removeDoubleQuotes(messageString)
		if (message == null || message.isEmpty()) {
			handleBadRequest("The new message is missing in the request body! Please add it as plain text.")
		}

		agentBase.dumpReport()
		logger.debug("Changing message to $message")
		agentBase.options.teamscaleServerOptions.message = message

		return Response.noContent().build()
	}

	/** Handles setting the revision.  */
	@PUT
	@Path("/revision")
	fun setRevision(revisionString: String): Response? {
		val revision = StringUtils.removeDoubleQuotes(revisionString)
		if (revision == null || revision.isEmpty()) {
			handleBadRequest("The new revision name is missing in the request body! Please add it as plain text.")
		}

		agentBase.dumpReport()
		logger.debug("Changing revision name to $revision")
		agentBase.options.teamscaleServerOptions.revision = revision

		return Response.noContent().build()
	}

	/** Handles setting the upload commit.  */
	@PUT
	@Path("/commit")
	fun setCommit(commitString: String): Response? {
		val commit = StringUtils.removeDoubleQuotes(commitString)
		if (commit == null || commit.isEmpty()) {
			handleBadRequest("The new upload commit is missing in the request body! Please add it as plain text.")
		}

		agentBase.dumpReport()
		agentBase.options.teamscaleServerOptions.commit = parse(commit)

		return Response.noContent().build()
	}

	private val revisionInfo: RevisionInfo
		/** Returns revision information for the Teamscale upload.  */
		get() {
			val server = agentBase.options.teamscaleServerOptions
			return RevisionInfo(server.commit, server.revision)
		}

	/**
	 * Handles bad requests to the endpoints.
	 */
	@Throws(BadRequestException::class)
	protected fun handleBadRequest(message: String?) {
		logger.error(message)
		throw BadRequestException(message)
	}

	companion object {
		/**
		 * The agentBase inject via [AgentResource.setAgent] or
		 * [com.teamscale.jacoco.agent.testimpact.TestwiseCoverageResource.setAgent].
		 */
		@JvmStatic
		protected lateinit var agentBase: AgentBase
	}
}
