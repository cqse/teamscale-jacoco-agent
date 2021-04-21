package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.options.ProjectRevision;
import com.teamscale.report.util.BashFileSkippingInputStream;
import org.conqat.lib.commons.collections.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility methods to extract certain properties from git.properties files in JARs. */
public class GitPropertiesLocatorUtils {

	/** Name of the git.properties file. */
	private static final String GIT_PROPERTIES_FILE_NAME = "git.properties";

	/** The git.properties key that holds the commit hash. */
	public static final String GIT_PROPERTIES_GIT_COMMIT_ID = "git.commit.id";

	/** The git.properties key that holds the Teamscale project name. */
	private static final String GIT_PROPERTIES_TEAMSCALE_PROJECT = "teamscale.project";

	/** Matches the path to the jar file in a jar:file: URL in regex group 1. */
	private static final Pattern JAR_URL_REGEX = Pattern.compile("jar:file:(.*?)!/.*", Pattern.CASE_INSENSITIVE);

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

	/**
	 * Tries to extract a file system path to a search root for the git.properties search. A search root is either a
	 * file system folder or a Jar file. If no such path can be extracted, returns null.
	 *
	 * @throws URISyntaxException under certain circumstances if parsing the URL fails. This should be treated the same
	 *                            as a null search result but the exception is preserved so it can be logged.
	 */
	public static File extractGitPropertiesSearchRoot(URL jarOrClassFolderUrl) throws URISyntaxException {
		String protocol = jarOrClassFolderUrl.getProtocol().toLowerCase();
		if (protocol.equals("file") && org.conqat.lib.commons.string.StringUtils.endsWithOneOf(
				jarOrClassFolderUrl.getPath().toLowerCase(), ".jar", ".war", ".ear", ".aar")) {
			return new File(jarOrClassFolderUrl.toURI());
		} else if (protocol.equals("jar")) {
			// used e.g. by Spring Boot. Example: jar:file:/home/k/demo.jar!/BOOT-INF/classes!/
			Matcher matcher = JAR_URL_REGEX.matcher(jarOrClassFolderUrl.toString());
			if (!matcher.matches()) {
				return null;
			}
			return new File(matcher.group(1));
		}
		return null;
	}

	/**
	 * Reads the 'teamscale.project' property value and the git SHA1 from the given jar file's git.properties. If no
	 * git.properties file can be found, returns null.
	 *
	 * @throws IOException                   If reading the jar file fails.
	 * @throws InvalidGitPropertiesException If a git.properties file is found but it is malformed.
	 */
	public static ProjectRevision getProjectRevisionFromGitProperties(
			File jarFile) throws IOException, InvalidGitPropertiesException {
		Pair<String, Properties> entryWithProperties = findGitPropertiesInJar(jarFile);
		if (entryWithProperties == null) {
			return null;
		}
		String revision = entryWithProperties.getSecond().getProperty(GIT_PROPERTIES_GIT_COMMIT_ID);
		String project = entryWithProperties.getSecond().getProperty(GIT_PROPERTIES_TEAMSCALE_PROJECT);
		if (StringUtils.isEmpty(revision) && StringUtils.isEmpty(project)) {
			throw new InvalidGitPropertiesException(
					"No entry or empty value for both '" + GIT_PROPERTIES_GIT_COMMIT_ID + "' and '" + GIT_PROPERTIES_TEAMSCALE_PROJECT + "' in " + jarFile + "." +
							"\nContents of " + GIT_PROPERTIES_FILE_NAME + ": " + entryWithProperties.getSecond()
							.toString()
			);
		}
		return new ProjectRevision(project, revision);
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
}
