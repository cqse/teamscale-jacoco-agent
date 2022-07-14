package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.options.ProjectRevision;
import com.teamscale.report.util.BashFileSkippingInputStream;
import org.conqat.lib.commons.collections.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO update JavaDoc in changed methods (esp. in those returning a list now)

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

	//TODO can we clean this up now? e.g. provide one regex for pruning paths
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
	public static List<String> getRevisionsFromGitProperties(
			File file, boolean isJarFile) throws IOException, InvalidGitPropertiesException {
		List<Pair<String, Properties>> entriesWithProperties = findGitPropertiesInFile(file, isJarFile);
		List<String> result = new ArrayList<>();
		for (Pair<String, Properties> entryWithProperties : entriesWithProperties) {
			result.add(getGitPropertiesValue(entryWithProperties.getSecond(), GIT_PROPERTIES_GIT_COMMIT_ID,
					entryWithProperties.getFirst(), file));
		}
		return result;
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
				// TODO in current Tomcat setup, only one git.properties file is found according to the log, Is there only one? or is there problem with log or the detection?
				File jarOrClassFolderFile = new File(jarOrClassFolderUrl.toURI());
				if (jarOrClassFolderFile.isDirectory() || org.conqat.lib.commons.string.StringUtils.endsWithOneOf(
						jarOrClassFolderUrl.getPath().toLowerCase(), ".jar", ".war", ".ear", ".aar")) {
					return Pair.createPair(new File(jarOrClassFolderUrl.toURI()), !jarOrClassFolderFile.isDirectory());
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
		return Pair.createPair(file, !file.isDirectory());
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
	public static List<ProjectRevision> getProjectRevisionsFromGitProperties(
			File file, boolean isJarFile) throws IOException, InvalidGitPropertiesException {
		List<Pair<String, Properties>> entriesWithProperties = findGitPropertiesInFile(file, isJarFile);
		List<ProjectRevision> result = new ArrayList<>();
		for (Pair<String, Properties> entryWithProperties : entriesWithProperties) {
			String revision = entryWithProperties.getSecond().getProperty(GIT_PROPERTIES_GIT_COMMIT_ID);
			String project = entryWithProperties.getSecond().getProperty(GIT_PROPERTIES_TEAMSCALE_PROJECT);
			if (StringUtils.isEmpty(revision) && StringUtils.isEmpty(project)) {
				throw new InvalidGitPropertiesException(
						"No entry or empty value for both '" + GIT_PROPERTIES_GIT_COMMIT_ID + "' and '" + GIT_PROPERTIES_TEAMSCALE_PROJECT + "' in " + file + "." +
								"\nContents of " + GIT_PROPERTIES_FILE_NAME + ": " + entryWithProperties.getSecond()
								.toString()
				);
			}
			result.add(new ProjectRevision(project, revision));
		}
		return result;
	}

	/** Returns a pair of the zipfile entry name and parsed properties, or null if no git.properties were found. */
	public static List<Pair<String, Properties>> findGitPropertiesInFile(
			File file, boolean isJarFile) throws IOException {
		File rootFile = file;
		String filePath = file.getPath();
		// TODO this can be improved, just call a method that strips all after .ear,.war,.aar,.jar
		if (isNestedInWar(filePath) || isNestedInEar(filePath) || isNestedInAar(filePath) || isNestedInFatJar(
				filePath)) {
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
			rootFile = Paths.get(firstPart).toFile();
		}
		if (isJarFile) {
			return findGitPropertiesInArchiveFile(rootFile);
		}
		return findGitPropertiesInDirectoryFile(rootFile);
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

	private static List<Pair<String, Properties>> findGitPropertiesInArchiveFile(File file) throws IOException {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(Files.newInputStream(file.toPath())))) {
			return findGitPropertiesInArchive(jarStream, file.getName());
		} catch (IOException e) {
			throw new IOException("Reading jar " + file.getAbsolutePath() + " for obtaining commit " +
					"descriptor from git.properties failed", e);
		}
	}

	// TODO hide recursive search behind feature toggle?
	private static List<Pair<String, Properties>> findGitPropertiesInDirectoryFile(
			File directoryFile) throws IOException {
		List<Pair<String, Properties>> result = new ArrayList<>();
		List<File> gitPropertiesFiles = FileSystemUtils.listFilesRecursively(directoryFile,
				file -> file.getName().equalsIgnoreCase(GIT_PROPERTIES_FILE_NAME));
		List<File> jarFiles = FileSystemUtils.listFilesRecursively(directoryFile,
				file -> file.getName().endsWith(JAR_FILE_ENDING));
		if (gitPropertiesFiles.isEmpty() && jarFiles.isEmpty()) {
			return result;
		}

		for (File gitPropertiesFile : gitPropertiesFiles) {
			try (InputStream is = Files.newInputStream(gitPropertiesFile.toPath())) {
				Properties gitProperties = new Properties();
				gitProperties.load(is);
				result.add(Pair.createPair(gitPropertiesFile.getName(), gitProperties));
			} catch (IOException e) {
				throw new IOException(
						"Reading directory " + gitPropertiesFile.getAbsolutePath() + " for obtaining commit " +
								"descriptor from git.properties failed", e);
			}
		}

		for (File jarFile : jarFiles) {
			JarInputStream is = new JarInputStream(Files.newInputStream(jarFile.toPath()));
			result.addAll(findGitPropertiesInArchive(is, jarFile.getName()));
		}
		return result;
	}

	// TODO hide recursive search behind feature toggle?

	/** Returns a pair of the zipfile entry name and parsed properties, or null if no git.properties were found. */
	static List<Pair<String, Properties>> findGitPropertiesInArchive(
			JarInputStream in, String archiveName) throws IOException {
		List<Pair<String, Properties>> result = new ArrayList<>();
		JarEntry entry;
		boolean isEmpty = true;

		while ((entry = in.getNextJarEntry()) != null) {
			isEmpty = false;
			String fullEntryName = archiveName + File.separator + entry.getName();
			if (Paths.get(entry.getName()).getFileName().toString().equalsIgnoreCase(GIT_PROPERTIES_FILE_NAME)) {
				Properties gitProperties = new Properties();
				gitProperties.load(in);
				result.add(Pair.createPair(fullEntryName, gitProperties));
			} else if (entry.getName().endsWith(JAR_FILE_ENDING)) {
				result.addAll(findGitPropertiesInArchive(new JarInputStream(in), fullEntryName));
			}
		}
		if (isEmpty) {
			throw new IOException(
					"No entries in Jar file " + archiveName + ". Is this a valid jar file?. If not, please report to CQSE.");
		}
		return result;
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
