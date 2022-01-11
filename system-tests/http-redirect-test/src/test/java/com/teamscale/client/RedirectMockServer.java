package com.teamscale.client;

import spark.Request;
import spark.Response;
import spark.Service;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static spark.Spark.exception;
import static spark.Spark.post;

/**
 * Mocks a redirect server: redirects all requests to another server.
 */
public class RedirectMockServer {

	public RedirectMockServer(int port, int redirectTo){
		Service.ignite().port(port);
		post("/", (Request request, Response response) -> {
			String url = request.url();
			url = url.replace(Integer.toString(port),Integer.toString(redirectTo));
			response.redirect(url,307);
			return response;
		});
		exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
	}

}
