package com.teamscale.jacoco.agent.git_properties;

import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.upload.delay.DelayedCommitDescriptorUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.util.BashFileSkippingInputStream;
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
 * e.g. to Teamscale, via a {@link DelayedCommitDescriptorUploader}.
 */
public class GitPropertiesLocator {

	/** Name of the git.properties file. */
	private static final String GIT_PROPERTIES_FILE_NAME = "git.properties";

	private final Logger logger = LoggingUtils.getLogger(GitPropertiesLocator.class);
	private final Executor executor;
	private String foundRevision = null;
	private File jarFileWithGitProperties = null;

	private final DelayedCommitDescriptorUploader store;

	public GitPropertiesLocator(DelayedCommitDescriptorUploader store) {
		// using a single threaded executor allows this class to be lock-free
		this(store, Executors
				.newSingleThreadExecutor(
						new DaemonThreadFactory(GitPropertiesLocator.class, "git.properties Jar scanner thread")));
	}

	/**
	 * Visible for testing. Allows tests to control the {@link Executor} in order to test the asynchronous functionality
	 * of this class.
	 */
	public GitPropertiesLocator(DelayedCommitDescriptorUploader store, Executor executor) {
		this.store = store;
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
			String revision = getCommitFromGitProperties(jarFile);
			if (revision == null) {
				logger.debug("No git.properties file found in {}", jarFile.toString());
				return;
			}

			if (foundRevision != null) {
				if (!foundRevision.equals(revision)) {
					logger.error(
							"Found inconsistent git.properties files: {} contained SHA1 {} while {} contained {}." +
									" Please ensure that all git.properties files of your application are consistent." +
									" Otherwise, you may" +
									" be uploading to the wrong commit which will result in incorrect coverage data" +
									" displayed in Teamscale. If you cannot fix the inconsistency, you can manually" +
									" specify a Jar/War/Ear/... file from which to read the correct git.properties" +
									" file with the agent's teamscale-git-properties-jar parameter.",
							jarFileWithGitProperties, foundRevision, jarFile, revision);
				}
				return;
			}

			logger.debug("Found git.properties file in {} and found commit descriptor {}", jarFile.toString(),
					revision);
			foundRevision = revision;
			jarFileWithGitProperties = jarFile;
			store.setCommitAndTriggerAsynchronousUpload(revision);
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
	public static String getCommitFromGitProperties(
			File jarFile) throws IOException, InvalidGitPropertiesException {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(new FileInputStream(jarFile)))) {
			return getCommitFromGitProperties(jarStream, jarFile);
		} catch (IOException e) {
			throw new IOException("Reading jar " + jarFile.getAbsolutePath() + " for obtaining commit " +
					"descriptor from git.properties failed", e);
		}
	}

	/** Visible for testing. */
	/*package*/
	static String getCommitFromGitProperties(
			JarInputStream jarStream, File jarFile) throws IOException, InvalidGitPropertiesException {
		JarEntry entry = jarStream.getNextJarEntry();
		while (entry != null) {
			if (Paths.get(entry.getName()).getFileName().toString().toLowerCase().equals(GIT_PROPERTIES_FILE_NAME)) {
				Properties gitProperties = new Properties();
				gitProperties.load(jarStream);
				return parseGitPropertiesJarEntry(entry.getName(), gitProperties, jarFile);
			}
			entry = jarStream.getNextJarEntry();
		}

		return null;
	}

	/** Visible for testing. */
	/*package*/
	static String parseGitPropertiesJarEntry(
			String entryName, Properties gitProperties, File jarFile) throws InvalidGitPropertiesException {
		String revision = gitProperties.getProperty("git.commit.id");
		if (StringUtils.isEmpty(revision)) {
			throw new InvalidGitPropertiesException(
					"No entry or empty value for 'git.commit.id' in " + entryName + " in " + jarFile + "." +
							"\nContents of " + GIT_PROPERTIES_FILE_NAME + ": " + gitProperties.toString()
			);
		}

		return revision;
	}
}
