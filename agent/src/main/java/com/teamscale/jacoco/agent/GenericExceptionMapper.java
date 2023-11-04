package com.teamscale.jacoco.agent;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Generates a {@link Response} for an exception.
 */
@javax.ws.rs.ext.Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

	@Override
	public Response toResponse(Throwable e) {
		Response.ResponseBuilder errorResponse = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
		errorResponse.type(MediaType.TEXT_PLAIN_TYPE);
		errorResponse.entity("Message: " + e.getMessage());
		return errorResponse.build();
	}
}
