package com.teamscale.jacoco.agent;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * The resource of the Jersey + Jetty http server holding all the endpoints specific for the {@link Agent}.
 */
@Path("/")
public class AgentResource extends ResourceBase {

	private static Agent agent;

	/**
	 * Static setter to inject the {@link Agent} to the resource.
	 */
	public static void setAgent(Agent agent) {
		AgentResource.agent = agent;
		ResourceBase.agentBase = agent;
	}

	/** Handles dumping a XML coverage report for coverage collected until now. */
	@POST
	@Path("/dump")
	public Response handleDump() {
		logger.debug("Dumping report triggered via HTTP request");
		agent.dumpReport();
		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

	/** Handles resetting of coverage. */
	@POST
	@Path("/reset")
	public Response handleReset() {
		logger.debug("Resetting coverage triggered via HTTP request");
		agent.controller.reset();
		return Response.status(HttpServletResponse.SC_NO_CONTENT, "").build();
	}

}
