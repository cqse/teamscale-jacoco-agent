package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.jacoco.agent.upload.delay.DelayedUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Searches a Jar/War/Ear/... file for a git.properties file in order to enable upload for the commit described therein,
 * e.g. to Teamscale, via a {@link DelayedUploader}.
 */
public class GitPropertiesLocator<T> implements IGitPropertiesLocator {

	/** The git.properties key that holds the commit time. */
	public static final String GIT_PROPERTIES_GIT_COMMIT_TIME = "git.commit.time";

	/** The git.properties key that holds the commit branch. */
	public static final String GIT_PROPERTIES_GIT_BRANCH = "git.branch";

	private final Logger logger = LoggingUtils.getLogger(GitPropertiesLocator.class);
	private final Executor executor;
	private T foundData = null;
	private File jarFileWithGitProperties = null;

	private final DelayedUploader<T> uploader;
	private final DataExtractor<T> dataExtractor;

	public GitPropertiesLocator(DelayedUploader<T> uploader, DataExtractor<T> dataExtractor) {
		// using a single threaded executor allows this class to be lock-free
		this(uploader, dataExtractor, Executors
				.newSingleThreadExecutor(
						new DaemonThreadFactory(GitPropertiesLocator.class, "git.properties Jar scanner thread")));
	}

	/**
	 * Visible for testing. Allows tests to control the {@link Executor} in order to test the asynchronous functionality
	 * of this class.
	 */
	public GitPropertiesLocator(DelayedUploader<T> uploader, DataExtractor<T> dataExtractor, Executor executor) {
		this.uploader = uploader;
		this.dataExtractor = dataExtractor;
		this.executor = executor;
	}

	/**
	 * Asynchronously searches the given jar file for a git.properties file.
	 */
	@Override
	public void searchFileForGitPropertiesAsync(File file, boolean isJarFile) {
		executor.execute(() -> searchFile(file, isJarFile));
	}

	private void searchFile(File file, boolean isJarFile) {
		logger.debug("Searching jar file {} for a single git.properties", file);
		try {
			T data = dataExtractor.extractData(file, isJarFile);
			if (data == null) {
				logger.debug("No git.properties file found in {}", file.toString());
				return;
			}

			if (foundData != null) {
				if (!foundData.equals(data)) {
					logger.error(
							"Found inconsistent git.properties files: {} contained data {} while {} contained {}." +
									" Please ensure that all git.properties files of your application are consistent." +
									" Otherwise, you may" +
									" be uploading to the wrong project/commit which will result in incorrect coverage data" +
									" displayed in Teamscale. If you cannot fix the inconsistency, you can manually" +
									" specify a Jar/War/Ear/... file from which to read the correct git.properties" +
									" file with the agent's teamscale-git-properties-jar parameter.",
							jarFileWithGitProperties, foundData, file, data);
				}
				return;
			}

			logger.debug("Found git.properties file in {} and found commit descriptor {}", file.toString(),
					data);
			foundData = data;
			jarFileWithGitProperties = file;
			uploader.setCommitAndTriggerAsynchronousUpload(data);
		} catch (IOException | InvalidGitPropertiesException e) {
			logger.error("Error during asynchronous search for git.properties in {}", file.toString(), e);
		}
	}

	/** Functional interface for data extraction from a jar file. */
	@FunctionalInterface
	public interface DataExtractor<T> {
		/** Extracts data from the JAR. */
		T extractData(File file, boolean isJarFile) throws IOException, InvalidGitPropertiesException;
	}
}
