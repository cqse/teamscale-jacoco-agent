package com.teamscale.jacoco.agent;

import static com.teamscale.jacoco.agent.upload.teamscale.TeamscaleUploader.RETRY_UPLOAD_FILE_SUFFIX;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jacoco.agent.rt.RT;
import org.slf4j.Logger;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.upload.teamscale.TeamscaleUploader;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.CoverageFile;

import okhttp3.HttpUrl;

/**
 * Base class for agent implementations. Handles logger shutdown, store creation
 * and instantiation of the {@link JacocoRuntimeController}.
 * <p>
 * Subclasses must handle dumping onto disk and uploading via the configured
 * uploader.
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
		retryUnsuccessfulUploads(options);
		if (options.getHttpServerPort() != null) {
			try {
				initServer();
			} catch (Exception e) {
				throw new IllegalStateException("Control server not started.", e);
			}
		}
	}

	/**
	 * If we have coverage that was leftover because of previously unsuccessful
	 * coverage uploads, we retry to upload them again with the same configuration
	 * as in the previous try.
	 *
	 * This method expects that the .properties file of the leftover coverage is
	 * constructed in a specific order.
	 * 
	 */
	private void retryUnsuccessfulUploads(AgentOptions options) {
		if (options.getOutputDirectory() == null) {
			return;
		}
		List<File> reuploadCandidates = FileSystemUtils.listFilesRecursively(
				options.getOutputDirectory().getParent().toFile(),
				filepath -> filepath.getName().contains(RETRY_UPLOAD_FILE_SUFFIX));
		for (File file : reuploadCandidates) {
			try {
				logger.info("Retrying previously unsuccessful coverage upload for file {}.", file);
				String[] config = FileSystemUtils.readFile(file).split("\n");
				TeamscaleServer server = new TeamscaleServer();
				server.url = HttpUrl.parse(StringUtils.stripPrefix(config[0], "url="));
				server.project = StringUtils.stripPrefix(config[1], "project=");
				server.userName = StringUtils.stripPrefix(config[2], "userName=");
				server.userAccessToken = StringUtils.stripPrefix(config[3], "userAccessToken=");
				server.partition = StringUtils.stripPrefix(config[4], "partition=");
				server.commit = CommitDescriptor.parse(StringUtils.stripPrefix(config[5], "commit="));
				String revision = StringUtils.stripPrefix(config[6], "revision=");
				if (revision.equals("null")) {
					revision = null;
				}
				server.revision = revision;
				server.setMessage(StringUtils.stripPrefix(config[7], "message=").replace("$nl$", "\n"));

				TeamscaleUploader uploader = new TeamscaleUploader(server);
				CoverageFile coverageFile = new CoverageFile(
						new File(StringUtils.stripSuffix(file.getAbsolutePath(), RETRY_UPLOAD_FILE_SUFFIX)));
				file.delete();
				uploader.upload(coverageFile);
			} catch (IOException e) {
				logger.error("Reading config of previously unsuccessful coverage upload failed.");
			}

		}
	}

	/**
	 * Lazily generated string representation of the command line arguments to print
	 * to the log.
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
	 * Starts the http server, which waits for information about started and
	 * finished tests.
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
	 * Registers a shutdown hook that stops the timer and dumps coverage a final
	 * time.
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

}
