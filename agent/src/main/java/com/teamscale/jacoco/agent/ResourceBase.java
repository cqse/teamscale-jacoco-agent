package com.teamscale.jacoco.agent;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.model.RevisionInfo;
import org.conqat.lib.commons.string.StringUtils;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Optional;


public abstract class ResourceBase {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	protected static AgentBase AGENT_BASE;

	/** JSON adapter for revision information. */
	private final JsonAdapter<RevisionInfo> revisionInfoJsonAdapter = new Moshi.Builder().build()
			.adapter(RevisionInfo.class);

	@GET
	@Path("/partition")
	public String getPartition() {
		return Optional.ofNullable(AGENT_BASE.options.getTeamscaleServerOptions().partition).orElse("");
	}

	@GET
	@Path("/message")
	public String getMessage() {
		return Optional.ofNullable(AGENT_BASE.options.getTeamscaleServerOptions().getMessage())
				.orElse("");
	}

	@GET
	@Path("/revision")
	public String getRevision() {
		return this.getRevisionInfo();
	}

	@GET
	@Path("/commit")
	public String getCommit() {
		return this.getRevisionInfo();
	}

	@PUT
	@Path("/partition")
	public Response setPartition(String partitionString) {
		String partition = StringUtils.removeDoubleQuotes(partitionString);
		if (partition == null || partition.isEmpty()) {
			handleBadRequest("The new partition name is missing in the request body! Please add it as plain text.");
		}

		logger.debug("Changing partition name to " + partition);
		AGENT_BASE.controller.setSessionId(partition);
		AGENT_BASE.options.getTeamscaleServerOptions().partition = partition;
		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

	@PUT
	@Path("/message")
	public Response setMessage(String messageString) {
		String message = StringUtils.removeDoubleQuotes(messageString);
		if (message == null || message.isEmpty()) {
			handleBadRequest("The new message is missing in the request body! Please add it as plain text.");
		}

		logger.debug("Changing message to " + message);
		AGENT_BASE.options.getTeamscaleServerOptions().setMessage(message);

		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

	@PUT
	@Path("/revision")
	public Response setRevision(String revisionString) {
		String revision = StringUtils.removeDoubleQuotes(revisionString);
		if (revision == null || revision.isEmpty()) {
			handleBadRequest("The new revision name is missing in the request body! Please add it as plain text.");
		}
		logger.debug("Changing revision name to " + revision);
		AGENT_BASE.options.getTeamscaleServerOptions().revision = revision;

		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

	@PUT
	@Path("/commit")
	public Response setCommit(String commitString) {
		String commit = StringUtils.removeDoubleQuotes(commitString);
		if (commit == null || commit.isEmpty()) {
			handleBadRequest("The new upload commit is missing in the request body! Please add it as plain text.");
		}
		AGENT_BASE.options.getTeamscaleServerOptions().commit = CommitDescriptor.parse(commit);


		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

	/** Returns revision information for the Teamscale upload. */
	private String getRevisionInfo() {
		TeamscaleServer server = AGENT_BASE.options.getTeamscaleServerOptions();
		return revisionInfoJsonAdapter.toJson(new RevisionInfo(server.commit, server.revision));
	}

	protected void handleBadRequest(String message) throws BadRequestException {
		logger.error(message);
		throw new BadRequestException(message);
	}

}
