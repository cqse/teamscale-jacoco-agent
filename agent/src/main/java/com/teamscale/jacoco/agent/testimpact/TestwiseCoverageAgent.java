/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jacoco.agent.rt.IAgent;

import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.AgentOptions;
import com.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;

import okhttp3.ResponseBody;
import spark.Request;
import spark.Response;
import spark.Service;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens
 * for test events.
 */
public class TestwiseCoverageAgent extends AgentBase {

	/** Path parameter placeholder used in the http requests. */
	private static final String TEST_ID_PARAMETER = ":testId";

	/** Map of port number to secondary agent service. */
	private final Map<Integer, IAgentService> secondaryAgents = new LinkedHashMap<>();

	/** Primary agent service (if any) or null. */
	private IAgentService primaryAgent = null;

	/** The agent options. */
	private AgentOptions options;

	/** The port the HTTP server listens at. */
	private final Service service;

	/** Constructor. */
	public TestwiseCoverageAgent(AgentOptions options, IAgent agent) throws IOException {
		super(options, agent);
		this.options = options;
		service = Service.ignite();
		initServer(determineFreePort());
	}

	/**
	 * Starts the HTTP server, which waits for information about started and
	 * finished tests.
	 * 
	 * @throws IOException
	 *             in case registration with the primary agent failed.
	 */
	private void initServer(int port) throws IOException {
		logger.info("Listening for test events on port {}.", port);

		service.initExceptionHandler(this::logInitException);
		service.port(port);

		service.get("/test", (request, response) -> controller.getSessionId());

		service.post("/test/start/" + TEST_ID_PARAMETER, this::handleTestStart);
		service.post("/test/end/" + TEST_ID_PARAMETER, this::handleTestEnd);

		service.post("/register", this::handleRegister);
		service.delete("/register", this::handleUnregister);

		if (port != options.getHttpServerPort()) {
			register();
		}

		service.awaitInitialization();
	}

	/**
	 * Returns the lowest unused local port that is equal or greater than the
	 * one provided in {@link #options}.
	 */
	private int determineFreePort() throws IOException {
		for (int currentPort = options.getHttpServerPort(); currentPort < 65535;) {
			try (ServerSocket s = new ServerSocket(currentPort)) {
				// found a free port. Jump out
				return currentPort;
			} catch (BindException e) {
				currentPort++;
			}
		}
		throw new IOException("Unable to determine a free server port.");
	}

	/** registers with a primary agent. */
	private void register() throws IOException {
		primaryAgent = IAgentService.create(options.getHttpServerPort());
		retrofit2.Response<ResponseBody> response = primaryAgent.register(getPort()).execute();
		if (response.code() != 204) {
			throw new IOException("Unable to register with primary agent at port: " + options.getHttpServerPort()
					+ ". HTTP status: " + response.code());
		}
	}

	/** Handles the start of a new test case by setting the session ID. */
	private String handleTestStart(Request request, Response response) {
		String testId = request.params(TEST_ID_PARAMETER);
		if (testId == null || testId.isEmpty()) {
			logger.error("Test name missing in " + request.url() + "!");

			response.status(400);
			return "Test name is missing!";
		}

		notifyTestStart(testId);
		logger.debug("Start test " + testId);

		// Dump and reset coverage so that we only record coverage that belongs
		// to this particular test case.
		controller.reset();
		controller.setSessionId(testId);

		response.status(204);
		return "";
	}

	/** Notifies all secondary agents about a test start event. */
	private void notifyTestStart(String testId) {
		new Thread(() -> {
			for (Entry<Integer, IAgentService> entry : secondaryAgents.entrySet()) {
				try {
					entry.getValue().signalTestStart(testId).execute();
				} catch (IOException e) {
					logger.warn("Error signaling test start to agent on port " + entry.getKey());
				}
			}
		}).start();
	}

	/** Handles the end of a test case by resetting the session ID. */
	private String handleTestEnd(Request request, Response response) throws DumpException {
		String testId = request.params(TEST_ID_PARAMETER);
		if (testId == null || testId.isEmpty()) {
			return error("Test name missing in " + request.url() + "!", response);
		}

		notifyTestEnd(testId);
		logger.debug("End test " + testId);

		controller.dump();

		response.status(204);
		return "";
	}

	/** Notifies all secondary agents about a test end event. */
	private void notifyTestEnd(String testId) {
		new Thread(() -> {
			for (Entry<Integer, IAgentService> entry : secondaryAgents.entrySet()) {
				try {
					entry.getValue().signalTestEnd(testId).execute();
				} catch (IOException e) {
					logger.warn("Error signaling test end to agent on port " + entry.getKey());
				}
			}
		}).start();
	}

	/** Registers the port from the registration query. */
	private String handleRegister(Request request, Response response) {
		try {
			int port = extractPortFromQuery(request);
			secondaryAgents.put(port, IAgentService.create(port));
		} catch (IllegalArgumentException e) {
			return error(e.getMessage(), response);
		}
		response.status(204);
		return "";
	}

	/** Registers the port from the registration query. */
	private String handleUnregister(Request request, Response response) {
		try {
			int port = extractPortFromQuery(request);
			IAgentService previous = secondaryAgents.remove(port);
			if (previous == null) {
				return error("No agent registered for port " + port, response);
			}
		} catch (IllegalArgumentException e) {
			return error(e.getMessage(), response);
		}
		response.status(204);
		return "";
	}

	/**
	 * Extracts and validates the port number from the request.
	 * 
	 * @throws IllegalArgumentException
	 *             if the port number is missing, not a number, or an invalid
	 *             port.
	 */
	private static int extractPortFromQuery(Request request) throws IllegalArgumentException {
		String portString = request.queryParams("port");
		if (portString == null || portString.isEmpty()) {
			throw new IllegalArgumentException("Port is missing!");
		}
		int port = Integer.parseInt(portString);
		if (port < 1024 || port > 65535) {
			throw new NumberFormatException("Port " + port + " is not a valid port.");
		}
		return port;
	}

	/**
	 * Logs the message to the error log, sets the response code to 400, and
	 * returns the message for chaining.
	 */
	private String error(String message, Response response) {
		logger.error(message);
		response.status(400);
		return message;
	}

	/** Logs errors that occurred during agent initialization. */
	private void logInitException(Exception e) {
		logger.error("Agent initialization failed", e);
	}

	/** Stops the HTTP service and unregisters from the primary agent. */
	@Override
	protected void prepareShutdown() {
		if (primaryAgent != null) {
			try {
				primaryAgent.unregister(getPort()).execute();
			} catch (IOException e) {
				logger.error("Unable to register from primary agent.");
			}
		}
		service.stop();
	}

	/** The port the HTTP server is listening at. */
	public int getPort() {
		return service.port();
	}
}
