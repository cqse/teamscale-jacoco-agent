package com.teamscale.jacoco.agent;

import static com.teamscale.jacoco.agent.upload.HttpZipUploaderBase.ADDITIONAL_METADATA_PROPERTIES_KEY;
import static com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryUploader.ARTIFACTORY_RETRY_UPLOAD_FILE_SUFFIX;
import static com.teamscale.jacoco.agent.upload.azure.AzureFileStorageUploader.AZURE_RETRY_UPLOAD_FILE_SUFFIX;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.COMMIT;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.MESSAGE;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.PARTITION;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.PROJECT;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.REVISION;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.URL;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.USER_ACCESS_TOKEN;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.USER_NAME;
import static com.teamscale.jacoco.agent.upload.teamscale.TeamscaleUploader.TEAMSCALE_RETRY_UPLOAD_FILE_SUFFIX;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

import com.google.common.base.Strings;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.EReportFormat;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.upload.UploaderException;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryConfig;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryUploader;
import com.teamscale.jacoco.agent.upload.azure.AzureFileStorageConfig;
import com.teamscale.jacoco.agent.upload.azure.AzureFileStorageUploader;
import com.teamscale.jacoco.agent.upload.teamscale.TeamscaleUploader;
import com.teamscale.jacoco.agent.util.AgentUtils;
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
	 */
	private void retryUnsuccessfulUploads(AgentOptions options) {
		Path outputPath = options.getOutputDirectory();
		if (outputPath == null) {
			// Default fallback
			outputPath = AgentUtils.getAgentDirectory().resolve("coverage");
		}
		List<File> reuploadCandidates = FileSystemUtils.listFilesRecursively(outputPath.getParent().toFile(),
				filepath -> filepath.getName().endsWith(TEAMSCALE_RETRY_UPLOAD_FILE_SUFFIX)
						|| filepath.getName().endsWith(AZURE_RETRY_UPLOAD_FILE_SUFFIX)
						|| filepath.getName().endsWith(ARTIFACTORY_RETRY_UPLOAD_FILE_SUFFIX));
		for (File file : reuploadCandidates) {
			reuploadCoverageFromPropertiesFile(file);
		}
	}

	private void reuploadCoverageFromPropertiesFile(File file) {
		String fileName = file.getName();
		logger.info("Retrying previously unsuccessful coverage upload for file {}.", file);
		try {
			InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()),
					StandardCharsets.UTF_8);
			Properties properties = new Properties();
			properties.load(reader);
			IUploader uploader;
			CoverageFile coverageFile;
			if (fileName.endsWith(ARTIFACTORY_RETRY_UPLOAD_FILE_SUFFIX)) {
				coverageFile = new CoverageFile(new File(
						StringUtils.stripSuffix(file.getAbsolutePath(), ARTIFACTORY_RETRY_UPLOAD_FILE_SUFFIX)));
				uploader = createArtifactoryUploader(properties);
			} else if (fileName.endsWith(AZURE_RETRY_UPLOAD_FILE_SUFFIX)) {
				coverageFile = new CoverageFile(
						new File(StringUtils.stripSuffix(file.getAbsolutePath(), AZURE_RETRY_UPLOAD_FILE_SUFFIX)));
				uploader = createAzureUploader(properties);
			} else {
				coverageFile = new CoverageFile(
						new File(StringUtils.stripSuffix(file.getAbsolutePath(), TEAMSCALE_RETRY_UPLOAD_FILE_SUFFIX)));
				uploader = createTeamscaleUploader(properties);
			}
			reader.close();
			uploader.upload(coverageFile);
			file.delete();
		} catch (IOException | UploaderException e) {
			logger.error("Reuploading coverage failed. " + e);
		}
	}

	private ArtifactoryUploader createArtifactoryUploader(Properties properties) {
		ArtifactoryConfig config = new ArtifactoryConfig();
		config.url = HttpUrl.parse(properties.getProperty(ArtifactoryConfig.ARTIFACTORY_URL_OPTION));
		config.user = properties.getProperty(ArtifactoryConfig.ARTIFACTORY_USER_OPTION);
		config.password = properties.getProperty(ArtifactoryConfig.ARTIFACTORY_PASSWORD_OPTION);
		config.legacyPath = Boolean
				.parseBoolean(properties.getProperty(ArtifactoryConfig.ARTIFACTORY_LEGACY_PATH_OPTION));
		config.zipPath = properties.getProperty(ArtifactoryConfig.ARTIFACTORY_ZIP_PATH_OPTION);
		config.pathSuffix = properties.getProperty(ArtifactoryConfig.ARTIFACTORY_PATH_SUFFIX);
		String revision = properties.getProperty(REVISION.name());
		String commitString = properties.getProperty(COMMIT.name());
		config.commitInfo = new ArtifactoryConfig.CommitInfo(revision, CommitDescriptor.parse(commitString));
		config.gitPropertiesCommitTimeFormat = DateTimeFormatter.ofPattern(
				properties.getProperty(ArtifactoryConfig.ARTIFACTORY_GIT_PROPERTIES_COMMIT_DATE_FORMAT_OPTION));
		config.apiKey = properties.getProperty(ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION);
		config.partition = properties.getProperty(ArtifactoryConfig.ARTIFACTORY_PARTITION);
		EReportFormat reportFormat = EReportFormat.valueOf(properties.getProperty("REPORT_FORMAT").toUpperCase());
		if (!config.hasAllRequiredFieldsSet()) {
			logger.error("Artifactory config misses required fields.");
		}
		return new ArtifactoryUploader(config, getAdditionalMetaDataPaths(properties), reportFormat);
	}

	private List<Path> getAdditionalMetaDataPaths(Properties properties) {
		List<Path> additionalMetaDataFiles = new ArrayList<>();
		for (int i = 0; i < Integer.parseInt(properties.getProperty(ADDITIONAL_METADATA_PROPERTIES_KEY)); i++) {
			additionalMetaDataFiles
					.add(Paths.get(properties.getProperty(ADDITIONAL_METADATA_PROPERTIES_KEY + "_" + i)));
		}
		return additionalMetaDataFiles;
	}

	private AzureFileStorageUploader createAzureUploader(Properties properties) throws UploaderException {
		AzureFileStorageConfig config = new AzureFileStorageConfig();
		config.url = HttpUrl.parse(properties.getProperty(URL.name()));
		config.accessKey = properties.getProperty(USER_ACCESS_TOKEN.name());
		return new AzureFileStorageUploader(config, getAdditionalMetaDataPaths(properties));
	}

	private TeamscaleUploader createTeamscaleUploader(Properties properties) {
		TeamscaleServer server = this.createTeamscaleServerFromProperties(properties);
		return new TeamscaleUploader(server);
	}

	/** Creates a TeamscalerServer instance from the provided Properties. */
	private TeamscaleServer createTeamscaleServerFromProperties(Properties properties) {
		TeamscaleServer server = new TeamscaleServer();
		server.url = HttpUrl.parse(properties.getProperty(URL.name()));
		server.project = properties.getProperty(PROJECT.name());
		server.userName = properties.getProperty(USER_NAME.name());
		server.userAccessToken = properties.getProperty(USER_ACCESS_TOKEN.name());
		server.partition = properties.getProperty(PARTITION.name());
		server.commit = CommitDescriptor.parse(properties.getProperty(COMMIT.name()));
		server.revision = Strings.emptyToNull(properties.getProperty(REVISION.name()));
		server.setMessage(properties.getProperty(MESSAGE.name()));
		return server;
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
