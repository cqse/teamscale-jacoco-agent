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

	private static final Pattern NESTED_JAR_REGEX = Pattern.compile("[jwea]ar:file:(.*?)\\*(.*)",
			Pattern.CASE_INSENSITIVE);

	/** File ending of Java web archive packages */
	public static final String WAR_FILE_ENDING = ".war";

	/** File ending of Java enterprise archive packages */
	public static final String EAR_FILE_ENDING = ".ear";

	/** File ending of Java archive packages */
	public static final String JAR_FILE_ENDING = ".jar";

	/** File ending of Android archive packages */
	public static final String AAR_FILE_ENDING = ".aar";

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
				// Used e.g. by Spring Boot. Example: jar:file:/home/k/demo.jar!/BOOT-INF/classes!/
				Matcher jarMatcher = JAR_URL_REGEX.matcher(jarOrClassFolderUrl.toString());
				if (jarMatcher.matches()) {
					return Pair.createPair(new File(jarMatcher.group(1)), true);
				}
				// Intentionally no break to handle ear and war files
			case "war":
			case "ear":
				// Used by some web applications and potentially fat jars.
				// Example: war:file:/Users/example/apache-tomcat/webapps/demo.war*/WEB-INF/lib/demoLib-1.0-SNAPSHOT.jar
				Matcher nestedMatcher = NESTED_JAR_REGEX.matcher(jarOrClassFolderUrl.toString());
				if (nestedMatcher.matches()) {
					return Pair.createPair(new File(nestedMatcher.group(1) + nestedMatcher.group(2)), true);
				}
				break;
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
	private static String extractArtefactUrl(URL jarOrClassFolderUrl) {
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
		String filePath = file.getPath();
		if (isNestedInWar(filePath) || isNestedInEar(filePath) || isNestedInAar(filePath) || isNestedInFatJar(
				filePath)) {
			return findGitPropertiesInNestedArchiveFile(file);
		} else if (isJarFile) {
			return findGitPropertiesInArchiveFile(file);
		}
		return findGitPropertiesInDirectoryFile(file);
	}

	private static boolean isNestedInWar(String filePath) {
		return filePath.contains(WAR_FILE_ENDING) && filePath.endsWith(JAR_FILE_ENDING);
	}

	private static boolean isNestedInFatJar(String filePath) {
		return filePath.contains(JAR_FILE_ENDING) &&
				filePath.indexOf(JAR_FILE_ENDING) != filePath.length() - JAR_FILE_ENDING.length();
	}

	private static boolean isNestedInEar(String filePath) {
		return filePath.contains(EAR_FILE_ENDING) && filePath.endsWith(JAR_FILE_ENDING);
	}

	private static boolean isNestedInAar(String filePath) {
		return filePath.contains(AAR_FILE_ENDING) && filePath.endsWith(JAR_FILE_ENDING);
	}

	private static Pair<String, Properties> findGitPropertiesInArchiveFile(File file) throws IOException {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(new FileInputStream(file)))) {
			return findGitPropertiesInArchive(jarStream);
		} catch (IOException e) {
			throw new IOException("Reading jar " + file.getAbsolutePath() + " for obtaining commit " +
					"descriptor from git.properties failed", e);
		}
	}

	/** Searches for a git.properties file inside a jar file that is nested inside a jar or war file. */
	static Pair<String, Properties> findGitPropertiesInNestedArchiveFile(File file) throws IOException {
		String filePath = file.getPath();
		int firstPartEndIndex;
		if (filePath.contains(WAR_FILE_ENDING)) {
			firstPartEndIndex = filePath.indexOf(WAR_FILE_ENDING) + WAR_FILE_ENDING.length();
		} else if (filePath.contains(EAR_FILE_ENDING)) {
			firstPartEndIndex = filePath.indexOf(EAR_FILE_ENDING) + EAR_FILE_ENDING.length();
		} else if (filePath.contains(AAR_FILE_ENDING)) {
			firstPartEndIndex = filePath.indexOf(AAR_FILE_ENDING) + AAR_FILE_ENDING.length();
		} else {
			firstPartEndIndex = filePath.indexOf(JAR_FILE_ENDING) + JAR_FILE_ENDING.length();
		}
		String firstPart = filePath.substring(0, firstPartEndIndex);
		String fileName = file.getName();
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(new FileInputStream(firstPart)))) {
			Pair<String, JarInputStream> nestedJar = findEntry(jarStream, fileName);
			if (nestedJar == null) {
				return null;
			}
			JarInputStream nestedJarStream = new JarInputStream(nestedJar.getSecond());
			return findGitPropertiesInArchive(nestedJarStream);

		} catch (IOException e) {
			throw new IOException("Reading jar " + firstPart + " for obtaining commit " +
					"descriptor from git.properties failed", e);
		}
	}

	/** Searches the given archive for the given name. */
	private static Pair<String, JarInputStream> findEntry(JarInputStream in, String name) throws IOException {
		JarEntry entry;
		while ((entry = in.getNextJarEntry()) != null) {
			if (Paths.get(entry.getName()).getFileName().toString().equalsIgnoreCase(name)) {
				return Pair.createPair(entry.getName(), in);
			}
		}
		return null;
	}

	private static Pair<String, Properties> findGitPropertiesInDirectoryFile(File directoryFile) throws IOException {
		List<File> gitPropertiesFiles = FileSystemUtils.listFilesRecursively(directoryFile,
				file -> file.getName().equalsIgnoreCase(GIT_PROPERTIES_FILE_NAME));
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
	static Pair<String, Properties> findGitPropertiesInArchive(
			JarInputStream jarStream) throws IOException {
		Pair<String, JarInputStream> propertiesEntry = findEntry(jarStream, GIT_PROPERTIES_FILE_NAME);
		if (propertiesEntry != null) {
			Properties gitProperties = new Properties();
			gitProperties.load(jarStream);
			return Pair.createPair(propertiesEntry.getFirst(), gitProperties);
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
							"\nContents of " + GIT_PROPERTIES_FILE_NAME + ": " + gitProperties
			);
		}

		return revision;
	}
}
