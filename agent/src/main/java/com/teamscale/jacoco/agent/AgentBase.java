package com.teamscale.jacoco.agent;

import com.google.common.annotations.VisibleForTesting;
import com.teamscale.client.ProxySystemProperties;
import com.teamscale.client.TeamscaleProxySystemProperties;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jacoco.agent.rt.RT;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;

/**
 * Base class for agent implementations. Handles logger shutdown, store creation and instantiation of the
 * {@link JacocoRuntimeController}.
 * <p>
 * Subclasses must handle dumping onto disk and uploading via the configured uploader.
 */
public abstract class AgentBase {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** Controls the JaCoCo runtime. */
	public final JacocoRuntimeController controller;

	/** The agent options. */
	protected AgentOptions options;

	private Server server;

	/** Constructor. */
	public AgentBase(AgentOptions options) throws IllegalStateException {
		this.options = options;

		try {
			controller = new JacocoRuntimeController(RT.getAgent());
		} catch (IllegalStateException e) {
			throw new IllegalStateException(
					"JaCoCo agent not started or there is a conflict with another JaCoCo agent on the classpath.", e);
		}
		logger.info("Starting JaCoCo agent for process {} with options: {}",
				ManagementFactory.getRuntimeMXBean().getName(), getOptionsObjectToLog());
		if (options.getHttpServerPort() != null) {
			try {
				initServer();
			} catch (Exception e) {
				logger.error("Could not start http server on port " + options.getHttpServerPort()
						+ ". Please check if the port is blocked.");
				throw new IllegalStateException("Control server not started.", e);
			}
		}
	}



	/**
	 * Lazily generated string representation of the command line arguments to print to the log.
	 */
	private Object getOptionsObjectToLog() {
		return new Object() {
			@Override
			public String toString() {
				if (options.shouldObfuscateSecurityRelatedOutputs()) {
					return options.getObfuscatedOptionsString();
				}
				return options.getOriginalOptionsString();
			}
		};
	}

	/**
	 * Starts the http server, which waits for information about started and finished tests.
	 */
	private void initServer() throws Exception {
		logger.info("Listening for test events on port {}.", options.getHttpServerPort());

		// Jersey Implementation
		ServletContextHandler handler = buildUsingResourceConfig();
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(10);
		threadPool.setDaemon(true);

		// Create a server instance and set the thread pool
		server = new Server(threadPool);
		// Create a server connector, set the port and add it to the server
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(options.getHttpServerPort());
		server.addConnector(connector);
		server.setHandler(handler);
		server.start();
	}

	private ServletContextHandler buildUsingResourceConfig() {
		ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		handler.setContextPath("/");

		ResourceConfig resourceConfig = initResourceConfig();
		handler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");
		return handler;
	}

	/**
	 * Initializes the {@link ResourceConfig} needed for the Jetty + Jersey Server
	 */
	protected abstract ResourceConfig initResourceConfig();

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final time.
	 */
	void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			stopServer();
			prepareShutdown();
			logger.info("CQSE JaCoCo agent successfully shut down.");
			PreMain.closeLoggingResources();
		}));
	}

	/** Stop the http server if it's running */
	void stopServer() {
		if (options.getHttpServerPort() != null) {
			try {
				server.stop();
			} catch (Exception e) {
				logger.error("Could not stop server so it is killed now.", e);
			} finally {
				server.destroy();
			}
		}
	}

	/** Called when the shutdown hook is triggered. */
	protected void prepareShutdown() {
		// Template method to be overridden by subclasses.
	}

	/**
	 * Dumps the current execution data, converts it, writes it to the output
	 * directory defined in {@link #options} and uploads it if an uploader is
	 * configured. Logs any errors, never throws an exception.
	 */
	public abstract void dumpReport();

}
