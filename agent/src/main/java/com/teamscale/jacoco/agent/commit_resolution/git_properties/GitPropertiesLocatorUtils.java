package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.options.ProjectRevision;
import com.teamscale.report.util.BashFileSkippingInputStream;
import org.conqat.lib.commons.collections.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
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
	public static String getRevisionFromGitProperties(
			File file, boolean isJarFile) throws IOException, InvalidGitPropertiesException {
		Pair<String, Properties> entryWithProperties = findGitPropertiesInFile(file, isJarFile);
		if (entryWithProperties == null) {
			return null;
		}
		return getGitPropertiesValue(entryWithProperties.getSecond(), GIT_PROPERTIES_GIT_COMMIT_ID,
				entryWithProperties.getFirst(), file);
	}

	/**
	 * Tries to extract a file system path to a search root for the git.properties search. A search root is either a
	 * file system folder or a Jar file. If no such path can be extracted, returns null.
	 *
	 * @throws URISyntaxException under certain circumstances if parsing the URL fails. This should be treated the same
	 *                            as a null search result but the exception is preserved so it can be logged.
	 */
	public static Pair<File, Boolean> extractGitPropertiesSearchRoot(
			URL jarOrClassFolderUrl) throws URISyntaxException, IOException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		String protocol = jarOrClassFolderUrl.getProtocol().toLowerCase();
		switch (protocol) {
			case "file":
				if (org.conqat.lib.commons.string.StringUtils.endsWithOneOf(
						jarOrClassFolderUrl.getPath().toLowerCase(), ".jar", ".war", ".ear", ".aar")) {
					return Pair.createPair(new File(jarOrClassFolderUrl.toURI()), true);
				}
				break;
			case "jar":
				// used e.g. by Spring Boot. Example: jar:file:/home/k/demo.jar!/BOOT-INF/classes!/
				Matcher matcher = JAR_URL_REGEX.matcher(jarOrClassFolderUrl.toString());
				if (!matcher.matches()) {
					return null;
				}
				return Pair.createPair(new File(matcher.group(1)), true);
			case "vfs":
				return getVfsContentFolder(jarOrClassFolderUrl);
			default:
				return null;
		}
		return null;
	}

	/**
	 * VFS (Virtual File System) protocol is used by JBoss EAP and Wildfly. Example of an URL:
	 * vfs:/content/helloworld.war/WEB-INF/classes
	 */
	private static Pair<File, Boolean> getVfsContentFolder(
			URL jarOrClassFolderUrl) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		// we obtain the URL of a specific class file as input, e.g.,
		// vfs:/content/helloworld.war/WEB-INF/classes
		// Next, we try to extract the artefact URL from it, e.g., vfs:/content/helloworld.war
		String artefactUrl = extractArtefactUrl(jarOrClassFolderUrl);

		Object virtualFile = new URL(artefactUrl).openConnection().getContent();
		Class<?> virtualFileClass = virtualFile.getClass();
		// obtain the physical location of the class file. It is created on demand in <jboss-installation-dir>/standalone/tmp/vfs
		Method getPhysicalFileMethod = virtualFileClass.getMethod("getPhysicalFile");
		File file = (File) getPhysicalFileMethod.invoke(virtualFile);
		return Pair.createPair(file, false);
	}

	/**
	 * Extracts the artefact URL (e.g., vfs:/content/helloworld.war/) from the full URL of the class file (e.g.,
	 * vfs:/content/helloworld.war/WEB-INF/classes).
	 */
	/* package */
	static String extractArtefactUrl(URL jarOrClassFolderUrl) {
		String url = jarOrClassFolderUrl.getPath().toLowerCase();
		String[] pathSegments = url.split("/");
		StringBuilder artefactUrlBuilder = new StringBuilder("vfs:");
		int segmentIdx = 0;
		while (segmentIdx < pathSegments.length) {
			String segment = pathSegments[segmentIdx];
			artefactUrlBuilder.append(segment);
			artefactUrlBuilder.append("/");
			if (org.conqat.lib.commons.string.StringUtils.endsWithOneOf(
					segment, ".jar", ".war", ".ear", ".aar")) {
				break;
			}
			segmentIdx += 1;
		}
		if (segmentIdx == pathSegments.length) {
			return url;
		}
		return artefactUrlBuilder.toString();
	}

	/**
	 * Reads the 'teamscale.project' property value and the git SHA1 from the given jar file's git.properties. If no
	 * git.properties file can be found, returns null.
	 *
	 * @throws IOException                   If reading the jar file fails.
	 * @throws InvalidGitPropertiesException If a git.properties file is found but it is malformed.
	 */
	public static ProjectRevision getProjectRevisionFromGitProperties(
			File file, boolean isJarFile) throws IOException, InvalidGitPropertiesException {
		Pair<String, Properties> entryWithProperties = findGitPropertiesInFile(file, isJarFile);
		if (entryWithProperties == null) {
			return null;
		}
		String revision = entryWithProperties.getSecond().getProperty(GIT_PROPERTIES_GIT_COMMIT_ID);
		String project = entryWithProperties.getSecond().getProperty(GIT_PROPERTIES_TEAMSCALE_PROJECT);
		if (StringUtils.isEmpty(revision) && StringUtils.isEmpty(project)) {
			throw new InvalidGitPropertiesException(
					"No entry or empty value for both '" + GIT_PROPERTIES_GIT_COMMIT_ID + "' and '" + GIT_PROPERTIES_TEAMSCALE_PROJECT + "' in " + file + "." +
							"\nContents of " + GIT_PROPERTIES_FILE_NAME + ": " + entryWithProperties.getSecond()
							.toString()
			);
		}
		return new ProjectRevision(project, revision);
	}

	/** Returns a pair of the zipfile entry name and parsed properties, or null if no git.properties were found. */
	public static Pair<String, Properties> findGitPropertiesInFile(
			File file, boolean isJarFile) throws IOException {
		if (isJarFile) {
			return findGitPropertiesInJarFile(file);
		}
		return findGitPropertiesInDirectoryFile(file);
	}

	private static Pair<String, Properties> findGitPropertiesInJarFile(File file) throws IOException {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(new FileInputStream(file)))) {
			return findGitPropertiesInJarFile(jarStream);
		} catch (IOException e) {
			throw new IOException("Reading jar " + file.getAbsolutePath() + " for obtaining commit " +
					"descriptor from git.properties failed", e);
		}
	}

	private static Pair<String, Properties> findGitPropertiesInDirectoryFile(File directoryFile) throws IOException {
		List<File> gitPropertiesFiles = FileSystemUtils.listFilesRecursively(directoryFile,
				file -> file.getName().toLowerCase().equals(GIT_PROPERTIES_FILE_NAME));
		if (gitPropertiesFiles.size() == 0) {
			return null;
		}
		if (gitPropertiesFiles.size() > 1) {
			throw new IllegalArgumentException(buildErrorMessage(directoryFile, gitPropertiesFiles));
		}
		File file = gitPropertiesFiles.get(0);
		try (InputStream is = new FileInputStream(file)) {
			Properties gitProperties = new Properties();
			gitProperties.load(is);
			return Pair.createPair(file.getName(), gitProperties);
		} catch (IOException e) {
			throw new IOException(
					"Reading directory " + file.getAbsolutePath() + " for obtaining commit " +
							"descriptor from git.properties failed", e);
		}
	}

	private static String buildErrorMessage(File directoryFile, List<File> gitPropertiesFiles) {
		StringBuilder errorMessage = new StringBuilder();
		errorMessage.append("There must be a single git.properties file in the directory ");
		errorMessage.append(directoryFile.toString());
		errorMessage.append(". Instead, found git.properties files in following locations: ");
		for (int i = 0; i < gitPropertiesFiles.size(); i++) {
			File gitPropertiesFile = gitPropertiesFiles.get(0);
			errorMessage.append(gitPropertiesFile.toString());
			if (i < gitPropertiesFiles.size() - 1) {
				errorMessage.append(", ");
			}
		}
		return errorMessage.toString();
	}

	/** Returns a pair of the zipfile entry name and parsed properties, or null if no git.properties were found. */
	static Pair<String, Properties> findGitPropertiesInJarFile(
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
