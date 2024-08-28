package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryConfig;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryMultiUploader;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import okhttp3.HttpUrl;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ArtifactoryMultiProjectPropertiesLocator implements IGitPropertiesLocator {

	private final Logger logger = LoggingUtils.getLogger(ArtifactoryMultiProjectPropertiesLocator.class);
	private final Executor executor;
	private final ArtifactoryMultiUploader uploader;
	private final boolean recursiveSearch;

	public ArtifactoryMultiProjectPropertiesLocator(ArtifactoryMultiUploader uploader, Executor executor,
			boolean recursiveSearch) {
		this.executor = executor;
		this.uploader = uploader;
		this.recursiveSearch = recursiveSearch;
	}

	public ArtifactoryMultiProjectPropertiesLocator(ArtifactoryMultiUploader uploader, boolean recursiveSearch) {
		// using a single threaded executor allows this class to be lock-free
		this(uploader, Executors
				.newSingleThreadExecutor(
						new DaemonThreadFactory(GitMultiProjectPropertiesLocator.class,
								"git.properties Jar scanner thread")), recursiveSearch);
	}

	/**
	 * Asynchronously searches the given jar file for git.properties files and adds a corresponding uploader to the
	 * multi-project uploader.
	 */
	@Override
	public void searchFileForGitPropertiesAsync(File file, boolean isJarFile) {
		executor.execute(() -> searchFile(file, isJarFile));
	}

	void searchFile(File file, boolean isJarFile) {
		logger.debug("Searching file {} for multiple git.properties", file.toString());
		try {
			List<String> urls = GitPropertiesLocatorUtils.getAllArtifactoryUrlsFromGitProperties(
					file, isJarFile, recursiveSearch);
			for (String url : urls) {
				ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
				artifactoryConfig.url = HttpUrl.parse(url);
				// TODO add username and password where do i get this?
				// TODO instead of null parse actual parameters, check where ArtifactoryUploader constructor is called and what is passed there
				uploader.addArtifactoryUploader(new ArtifactoryUploader(artifactoryConfig, null, null));
			}

		} catch (IOException e) {
			logger.error("Error trying to find artifactory urls in directory {}", file, e);
		}


	}
}
