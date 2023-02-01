package com.teamscale.jacoco.agent;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class AgentResource extends ResourceBase {

	private static Agent agent;

	public static void setAgent(Agent agent) {
		AgentResource.agent = agent;
		ResourceBase.AGENT_BASE = agent;
	}

	@POST
	@Path("/dump")
	public Response handleDump() {
		logger.debug("Dumping report triggered via HTTP request");
		agent.dumpReport();
		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

	@POST
	@Path("/reset")
	public Response handleReset() {
		logger.debug("Resetting coverage triggered via HTTP request");
		agent.controller.reset();
		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

}
