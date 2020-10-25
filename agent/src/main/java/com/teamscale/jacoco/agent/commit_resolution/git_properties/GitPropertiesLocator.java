package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.upload.delay.DelayedUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.util.BashFileSkippingInputStream;
import org.conqat.lib.commons.collections.Pair;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Searches a Jar/War/Ear/... file for a git.properties file in order to enable upload for the commit described therein,
 * e.g. to Teamscale, via a {@link DelayedUploader}.
 */
public class GitPropertiesLocator<T> {

	/** Name of the git.properties file. */
	private static final String GIT_PROPERTIES_FILE_NAME = "git.properties";
	/** The git.properties key that holds the commit hash. */
	public static final String GIT_PROPERTIES_GIT_COMMIT_ID = "git.commit.id";

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
	public void searchJarFileForGitPropertiesAsync(File jarFile) {
		executor.execute(() -> searchJarFile(jarFile));
	}

	private void searchJarFile(File jarFile) {
		try {
			T data = dataExtractor.extractData(jarFile);
			if (data == null) {
				logger.debug("No git.properties file found in {}", jarFile.toString());
				return;
			}

			if (foundData != null) {
				if (!foundData.equals(data)) {
					logger.error(
							"Found inconsistent git.properties files: {} contained data {} while {} contained {}." +
									" Please ensure that all git.properties files of your application are consistent." +
									" Otherwise, you may" +
									" be uploading to the wrong commit which will result in incorrect coverage data" +
									" displayed in Teamscale. If you cannot fix the inconsistency, you can manually" +
									" specify a Jar/War/Ear/... file from which to read the correct git.properties" +
									" file with the agent's teamscale-git-properties-jar parameter.",
							jarFileWithGitProperties, foundData, jarFile, data);
				}
				return;
			}

			logger.debug("Found git.properties file in {} and found commit descriptor {}", jarFile.toString(),
					data);
			foundData = data;
			jarFileWithGitProperties = jarFile;
			uploader.setCommitAndTriggerAsynchronousUpload(data);
		} catch (IOException | InvalidGitPropertiesException e) {
			logger.error("Error during asynchronous search for git.properties in {}", jarFile.toString(), e);
		}
	}

	/**
	 * Reads the git SHA1 from the given jar file's git.properties and builds a commit descriptor out of it. If no
	 * git.properties file can be found, returns null.
	 *
	 * @throws IOException                   If reading the jar file fails.
	 * @throws InvalidGitPropertiesException If a git.properties file is found but it is malformed.
	 */
	public static String getRevisionFromGitProperties(File jarFile) throws IOException, InvalidGitPropertiesException {
		Pair<String, Properties> entryWithProperties = findGitPropertiesInJar(jarFile);
		if (entryWithProperties == null) {
			return null;
		}
		return getGitPropertiesValue(entryWithProperties.getSecond(), GIT_PROPERTIES_GIT_COMMIT_ID,
				entryWithProperties.getFirst(), jarFile);
	}

	/** Returns a pair of the zipfile entry name and parsed properties, or null if no git.properties were found. */
	public static Pair<String, Properties> findGitPropertiesInJar(
			File jarFile) throws IOException {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(new FileInputStream(jarFile)))) {
			return findGitPropertiesInJar(jarStream);
		} catch (IOException e) {
			throw new IOException("Reading jar " + jarFile.getAbsolutePath() + " for obtaining commit " +
					"descriptor from git.properties failed", e);
		}
	}

	/** Returns a pair of the zipfile entry name and parsed properties, or null if no git.properties were found. */
	static Pair<String, Properties> findGitPropertiesInJar(
			JarInputStream jarStream) throws IOException {
		JarEntry entry = jarStream.getNextJarEntry();
		while (entry != null) {
			if (Paths.get(entry.getName()).getFileName().toString().toLowerCase().equals(GIT_PROPERTIES_FILE_NAME)) {
				Properties gitProperties = new Properties();
				gitProperties.load(jarStream);
				return Pair.createPair(entry.getName(), gitProperties);
			}
			entry = jarStream.getNextJarEntry();
		}

		return null;
	}

	/** Returns a value from a git properties file. */
	public static String getGitPropertiesValue(
			Properties gitProperties, String key, String entryName, File jarFile) throws InvalidGitPropertiesException {
		String revision = gitProperties.getProperty(key);
		if (StringUtils.isEmpty(revision)) {
			throw new InvalidGitPropertiesException(
					"No entry or empty value for '" + key + "' in " + entryName + " in " + jarFile + "." +
							"\nContents of " + GIT_PROPERTIES_FILE_NAME + ": " + gitProperties.toString()
			);
		}

		return revision;
	}

	/** Functional interface for data extraction from a jar file. */
	@FunctionalInterface
	public interface DataExtractor<T> {
		/** Extracts data from the JAR. */
		T extractData(File jarFile) throws IOException, InvalidGitPropertiesException;
	}
}
