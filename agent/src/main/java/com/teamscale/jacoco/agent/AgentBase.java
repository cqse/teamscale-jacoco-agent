package com.teamscale.jacoco.agent;

import com.teamscale.jacoco.agent.testimpact.IAgentService;
import com.teamscale.jacoco.agent.util.LoggingUtils;

import org.jacoco.agent.rt.IAgent;
import eu.cqse.teamscale.client.HttpUtils;
import okhttp3.ResponseBody;
import spark.Request;
import spark.Response;
import spark.Service;

import org.jacoco.agent.rt.RT;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for agent implementations. Handles logger shutdown, store creation and instantiation of the {@link
 * JacocoRuntimeController}.
 * <p>
 * Subclasses must handle dumping into the store.
 */
public abstract class AgentBase {

	/** The maximum port number for the agent to listen at. */
	private static final int MAX_PORT_NUMBER = 65535;

	/** The threshold for {@link #serviceInitializationCounter}. */
	private static final int MAX_INIT_COUNT = 5;

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** Controls the JaCoCo runtime. */
	protected final JacocoRuntimeController controller;

	/** The agent options. */
	protected AgentOptions options;

	private static LoggingUtils.LoggingResources loggingResources;

	/** Map of port number to secondary agent service. */
	protected final Map<Integer, IAgentService> secondaryAgents = new LinkedHashMap<>();

	/** Primary agent service (if any) or null. */
	private IAgentService primaryAgent = null;

	/** The service for the HTTP API. */
	protected Service service;

	/** The number of times we tried to initialize {@link #service}. */
	public int serviceInitializationCounter = 0;

	/** Constructor. */
	public AgentBase(AgentOptions options, IAgent jacocoAgent) throws IllegalStateException, IOException {
		this.options = options;
		try {
			controller = new JacocoRuntimeController(jacocoAgent);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(
					"JaCoCo agent not started or there is a conflict with another JaCoCo agent on the classpath.", e);
		}

		logger.info("Starting JaCoCo agent with options: {}", options.getOriginalOptionsString());
		if (options.getHttpServerPort() != null) {
			initServer(determineFreePort());
		}
	}

	/**
	 * Starts the http server, which waits for information about started and finished tests.
	 */
	private void initServer(int port) throws IOException {
		logger.info("Listening for test events on port {}.", port);
		serviceInitializationCounter++;

		service = Service.ignite();
		service.initExceptionHandler(this::handleInitException);
		service.port(port);
		
		service.post("/register", this::handleRegister);
		service.delete("/register", this::handleUnregister);
		
		initServerEndpoints();
		
		if (port != options.getHttpServerPort()) {
			registerWithPrimaryAgent();
		}
	}

	/**
	 * Returns the lowest unused local port that is equal or greater than the
	 * one provided in {@link #options}.
	 */
	private int determineFreePort() throws IOException {
		for (int currentPort = options.getHttpServerPort(); currentPort < MAX_PORT_NUMBER;) {
			try (ServerSocket s = new ServerSocket(currentPort)) {
				// We found a free port. This releases the socket on that port,
				// so our Spark service can claim it. In case of concurrency
				// issues, the service unable to claim the port will fail to
				// initialize and call logInitExceptionAndExit.
				return currentPort;
			} catch (BindException e) {
				currentPort++;
			}
		}
		throw new IOException("Unable to determine a free server port.");
	}
	
	/** Adds the endpoints that are available in the implemented mode. */
	protected abstract void initServerEndpoints();
	
	/** registers with a primary agent. */
	private void registerWithPrimaryAgent() throws IOException {
		primaryAgent = IAgentService.create(options.getHttpServerPort());
		retrofit2.Response<ResponseBody> response = primaryAgent.register(getPort()).execute();
		if (response.code() != 204) {
			throw new IOException("Unable to register with primary agent at port: " + options.getHttpServerPort()
					+ ". HTTP status: " + response.code());
		}
	}
	
	/** Handles registrations from secondary agents. */
	private String handleRegister(Request request, Response response) {
		if (primaryAgent != null) {
			return error(response, "Cannot register with a secondary agent.");
		}

		try {
			int port = extractPortFromQuery(request);
			secondaryAgents.put(port, IAgentService.create(port));
		} catch (IllegalArgumentException e) {
			return error(response, e.getMessage(), e);
		}

		response.status(204);
		return "";
	}

	/** Handles unregistrations from secondary agents. */
	private String handleUnregister(Request request, Response response) {
		if (primaryAgent != null) {
			return error(response, "Cannot unregister from a secondary agent.");
		}

		try {
			int port = extractPortFromQuery(request);
			IAgentService previous = secondaryAgents.remove(port);
			if (previous == null) {
				return error(response, "No secondary agent registered for port {}", port);
			}
		} catch (IllegalArgumentException e) {
			return error(response, e.getMessage(), e);
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
		if (port < 1024 || port >= MAX_PORT_NUMBER) {
			throw new NumberFormatException("Port " + port + " is not a valid port.");
		}
		return port;
	}


	/** Called by the actual premain method once the agent is isolated from the rest of the application. */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		DelayedLogger delayedLogger = new DelayedLogger();
		try {
			agentOptions = AgentOptionsParser.parse(options, delayedLogger);
		} catch (AgentOptionParseException e) {
			try (LoggingUtils.LoggingResources ignored = LoggingUtils.initializeDefaultLogging()) {
				Logger logger = LoggingUtils.getLogger(PreMain.class);
				delayedLogger.logTo(logger);
				logger.error("Failed to parse agent options: " + e.getMessage(), e);
				System.err.println("Failed to parse agent options: " + e.getMessage());
				throw e;
			}
		}

		loggingResources = LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		Logger logger = LoggingUtils.getLogger(Agent.class);
		delayedLogger.logTo(logger);

		HttpUtils.setShouldValidateSsl(agentOptions.validateSsl);

		logger.info("Starting JaCoCo's agent");
		org.jacoco.agent.rt.internal_035b120.PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		AgentBase agent = agentOptions.createAgent(RT.getAgent());
		agent.registerShutdownHook();
	}

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final time.
	 */
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (options.getHttpServerPort() != null) {
				service.stop();
			}
			prepareShutdown();
			logger.info("CQSE JaCoCo agent successfully shut down.");
			loggingResources.close();
		}));
	}

	/** Unregisters from the primary agent. */
	protected void prepareShutdown() {
		if (primaryAgent != null) {
			try {
				primaryAgent.unregister(getPort()).execute();
			} catch (IOException e) {
				logger.error("Unable to unregister from primary agent.", e);
			}
		}
	}
	
	/**
	 * Handles errors that occurred during agent initialization. The most
	 * typical case is concurrency issues in port assignment, in which case we
	 * try to find an unused port up to five times. Other exceptions are handled
	 * by exiting the process, since we would lose data otherwise.
	 */
	private void handleInitException(Exception e) {
		try {
			if (e instanceof BindException && serviceInitializationCounter < MAX_INIT_COUNT) {
				logger.warn("Web server port collision. Retrying...");
				initServer(determineFreePort());
				return;
			}
		} catch (IOException e1) {
			// Nothing to do, since we fail below anyway.
		}
		logger.error("Unrecoverable initialization error. Exiting.");
		System.exit(1);
	}

	/** The port the HTTP server is listening at. */
	public int getPort() {
		return service.port();
	}
	
	/**
	 * Logs the message to the error log, sets the response code to 400, and
	 * returns the message for chaining. As noted in the
	 * <a href="https://www.slf4j.org/faq.html#paramException">FAQ</a>, the
	 * arguments may be string parameters, which are concatenated into the
	 * string if error logging is enabled, and the last argument may be an
	 * exception/throwable, whose stack trace is then logged as well.
	 */
	protected String error(Response response, String message, Object... arguments) {
		logger.error(message, arguments);
		response.status(400);
		return message;
	}
}
