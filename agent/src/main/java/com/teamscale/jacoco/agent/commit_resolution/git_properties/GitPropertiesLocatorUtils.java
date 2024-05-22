package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.options.ProjectAndCommit;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility methods to extract certain properties from git.properties files in archives and folders. */
public class GitPropertiesLocatorUtils {

	/** Name of the git.properties file. */
	public static final String GIT_PROPERTIES_FILE_NAME = "git.properties";

	/** The git.properties key that holds the commit time. */
	public static final String GIT_PROPERTIES_GIT_COMMIT_TIME = "git.commit.time";

	/** The git.properties key that holds the commit branch. */
	public static final String GIT_PROPERTIES_GIT_BRANCH = "git.branch";

	/** The git.properties key that holds the commit hash. */
	static final String GIT_PROPERTIES_GIT_COMMIT_ID = "git.commit.id";

	/**
	 * Alternative git.properties key that might also hold the commit hash, depending on the Maven git-commit-id plugin
	 * configuration.
	 */
	private static final String GIT_PROPERTIES_GIT_COMMIT_ID_FULL = "git.commit.id.full";

	/**
	 * You can provide a teamscale timestamp in git.properties files to overwrite the revision. See <a
	 * href="https://cqse.atlassian.net/browse/TS-38561">TS-38561</a>.
	 */
	static final String GIT_PROPERTIES_TEAMSCALE_TIMESTAMP = "teamscale.timestamp";

	/** The git.properties key that holds the Teamscale project name. */
	private static final String GIT_PROPERTIES_TEAMSCALE_PROJECT = "teamscale.project";

	/** Matches the path to the jar file in a jar:file: URL in regex group 1. */
	private static final Pattern JAR_URL_REGEX = Pattern.compile("jar:(?:file|nested):(.*?)!.*",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern NESTED_JAR_REGEX = Pattern.compile("[jwea]ar:file:(.*?)\\*(.*)",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Defined in <a
	 * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/blob/ac05b16dfdcc2aebfa45ad3af4acf1254accffa3/src/main/java/pl/project13/maven/git/GitCommitIdMojo.java#L522">GitCommitIdMojo</a>
	 */
	private static final String GIT_PROPERTIES_DEFAULT_MAVEN_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

	/**
	 * Defined in <a
	 * href="https://github.com/n0mer/gradle-git-properties/blob/bb1c3353bb570495644b6c6c75e211296a8354fc/src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy#L68">GitPropertiesPlugin</a>
	 */
	private static final String GIT_PROPERTIES_DEFAULT_GRADLE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	/** File ending of Java archive packages */
	public static final String JAR_FILE_ENDING = ".jar";

	/**
	 * Reads the git SHA1 and branch and timestamp from the given jar file's git.properties and builds a commit
	 * descriptor out of it. If no git.properties file can be found, returns null.
	 *
	 * @throws IOException                   If reading the jar file fails.
	 * @throws InvalidGitPropertiesException If a git.properties file is found but it is malformed.
	 */
	public static List<CommitInfo> getCommitInfoFromGitProperties(
			File file, boolean isJarFile, boolean recursiveSearch) throws IOException, InvalidGitPropertiesException {
		List<Pair<String, Properties>> entriesWithProperties = findGitPropertiesInFile(file, isJarFile,
				recursiveSearch);
		List<CommitInfo> result = new ArrayList<>();
		for (Pair<String, Properties> entryWithProperties : entriesWithProperties) {
			CommitInfo commitInfo = getCommitInfoFromGitProperties(entryWithProperties.getSecond(),
					entryWithProperties.getFirst(), file);
			result.add(commitInfo);
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
					return Pair.createPair(new File(nestedMatcher.group(1)), true);
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
	 * Reads the 'teamscale.project' property values and the git SHA1s or branch + timestamp from all git.properties
	 * files contained in the provided folder or archive file.
	 *
	 * @throws IOException                   If reading the jar file fails.
	 * @throws InvalidGitPropertiesException If a git.properties file is found but it is malformed.
	 */
	public static List<ProjectAndCommit> getProjectRevisionsFromGitProperties(
			File file, boolean isJarFile, boolean recursiveSearch) throws IOException, InvalidGitPropertiesException {
		List<Pair<String, Properties>> entriesWithProperties = findGitPropertiesInFile(file, isJarFile,
				recursiveSearch);
		List<ProjectAndCommit> result = new ArrayList<>();
		for (Pair<String, Properties> entryWithProperties : entriesWithProperties) {
			CommitInfo commitInfo = getCommitInfoFromGitProperties(entryWithProperties.getSecond(),
					entryWithProperties.getFirst(), file);
			String project = entryWithProperties.getSecond().getProperty(GIT_PROPERTIES_TEAMSCALE_PROJECT);
			if (StringUtils.isEmpty(project)) {
				// commitInfo is not empty, corresponding checks are implemented in getCommitInfoForGitProperties
				throw new InvalidGitPropertiesException(
						"No entry or empty value for both '" + GIT_PROPERTIES_GIT_COMMIT_ID + "'/'" + GIT_PROPERTIES_GIT_COMMIT_ID_FULL +
								"' and '" + GIT_PROPERTIES_TEAMSCALE_PROJECT + "' in " + file + "." +
								"\nContents of " + GIT_PROPERTIES_FILE_NAME + ": " + entryWithProperties.getSecond()
								.toString()
				);
			}
			result.add(new ProjectAndCommit(project, commitInfo));
		}
		return result;
	}

	/**
	 * Returns pairs of paths to git.properties files and their parsed properties found in the provided folder or
	 * archive file. Nested jar files will also be searched recursively if specified.
	 */
	public static List<Pair<String, Properties>> findGitPropertiesInFile(
			File file, boolean isJarFile, boolean recursiveSearch) throws IOException {
		if (isJarFile) {
			return findGitPropertiesInArchiveFile(file, recursiveSearch);
		}
		return findGitPropertiesInDirectoryFile(file, recursiveSearch);
	}

	/**
	 * Searches for git properties in jar/war/ear/aar files
	 */
	private static List<Pair<String, Properties>> findGitPropertiesInArchiveFile(File file,
			boolean recursiveSearch) throws IOException {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(Files.newInputStream(file.toPath())))) {
			return findGitPropertiesInArchive(jarStream, file.getName(), recursiveSearch);
		} catch (IOException e) {
			throw new IOException("Reading jar " + file.getAbsolutePath() + " for obtaining commit " +
					"descriptor from git.properties failed", e);
		}
	}

	/**
	 * Searches for git.properties file in the given folder
	 *
	 * @param recursiveSearch If enabled, git.properties files will also be searched in jar files
	 */
	private static List<Pair<String, Properties>> findGitPropertiesInDirectoryFile(
			File directoryFile, boolean recursiveSearch) throws IOException {
		List<Pair<String, Properties>> result = new ArrayList<>(findGitPropertiesInFolder(directoryFile));

		if (recursiveSearch) {
			result.addAll(findGitPropertiesInNestedJarFiles(directoryFile));
		}

		return result;
	}

	/**
	 * Finds all jar files in the given folder and searches them recursively for git.properties
	 */
	private static List<Pair<String, Properties>> findGitPropertiesInNestedJarFiles(
			File directoryFile) throws IOException {
		List<Pair<String, Properties>> result = new ArrayList<>();
		List<File> jarFiles = FileSystemUtils.listFilesRecursively(directoryFile,
				file -> file.getName().endsWith(JAR_FILE_ENDING));
		for (File jarFile : jarFiles) {
			JarInputStream is = new JarInputStream(Files.newInputStream(jarFile.toPath()));
			String relativeFilePath = directoryFile.getName() + File.separator + directoryFile.toPath()
					.relativize(jarFile.toPath());
			result.addAll(findGitPropertiesInArchive(is, relativeFilePath, true));
		}
		return result;
	}

	/**
	 * Searches for git.properties files in the given folder
	 */
	private static List<Pair<String, Properties>> findGitPropertiesInFolder(File directoryFile) throws IOException {
		List<Pair<String, Properties>> result = new ArrayList<>();
		List<File> gitPropertiesFiles = FileSystemUtils.listFilesRecursively(directoryFile,
				file -> file.getName().equalsIgnoreCase(GIT_PROPERTIES_FILE_NAME));
		for (File gitPropertiesFile : gitPropertiesFiles) {
			try (InputStream is = Files.newInputStream(gitPropertiesFile.toPath())) {
				Properties gitProperties = new Properties();
				gitProperties.load(is);
				String relativeFilePath = directoryFile.getName() + File.separator + directoryFile.toPath()
						.relativize(gitPropertiesFile.toPath());
				result.add(Pair.createPair(relativeFilePath, gitProperties));
			} catch (IOException e) {
				throw new IOException(
						"Reading directory " + gitPropertiesFile.getAbsolutePath() + " for obtaining commit " +
								"descriptor from git.properties failed", e);
			}
		}
		return result;
	}

	/**
	 * Returns pairs of paths to git.properties files and their parsed properties found in the provided JarInputStream.
	 * Nested jar files will also be searched recursively if specified.
	 */
	static List<Pair<String, Properties>> findGitPropertiesInArchive(
			JarInputStream in, String archiveName, boolean recursiveSearch) throws IOException {
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
			} else if (entry.getName().endsWith(JAR_FILE_ENDING) && recursiveSearch) {
				result.addAll(findGitPropertiesInArchive(new JarInputStream(in), fullEntryName, true));
			}
		}
		if (isEmpty) {
			throw new IOException(
					"No entries in Jar file " + archiveName + ". Is this a valid jar file?. If so, please report to CQSE.");
		}
		return result;
	}

	/**
	 * Returns the CommitInfo (revision & branch + timestmap) from a git properties file. The revision can be either in
	 * {@link #GIT_PROPERTIES_GIT_COMMIT_ID} or {@link #GIT_PROPERTIES_GIT_COMMIT_ID_FULL}. The branch and timestamp in
	 * {@link #GIT_PROPERTIES_GIT_BRANCH} + {@link #GIT_PROPERTIES_GIT_COMMIT_TIME} or in
	 * {@link #GIT_PROPERTIES_TEAMSCALE_TIMESTAMP}
	 */
	public static CommitInfo getCommitInfoFromGitProperties(
			Properties gitProperties, String entryName, File jarFile) throws InvalidGitPropertiesException {
		return getCommitInfoFromGitProperties(gitProperties, entryName, jarFile, DateTimeFormatter.ofPattern(
				String.format("[%s][%s]", GIT_PROPERTIES_DEFAULT_MAVEN_DATE_FORMAT,
						GIT_PROPERTIES_DEFAULT_GRADLE_DATE_FORMAT)));
	}

	/**
	 * Same as {@link #getCommitInfoFromGitProperties(Properties, String, File)} but with the option to provide a
	 * DateTimeFormatter that is used to parse the times in {@link #GIT_PROPERTIES_GIT_COMMIT_TIME} and
	 * {@link #GIT_PROPERTIES_TEAMSCALE_TIMESTAMP}
	 */
	public static CommitInfo getCommitInfoFromGitProperties(
			Properties gitProperties, String entryName, File jarFile,
			DateTimeFormatter dateTimeFormatter) throws InvalidGitPropertiesException {

		// Get Revision
		String revision = getRevisionFromGitProperties(gitProperties);

		// Get branch and timestamp from git.commit.branch and git.commit.id
		CommitDescriptor commitDescriptor = getCommitDescriptorFromDefaultGitPropertyValues(gitProperties, entryName,
				jarFile,
				dateTimeFormatter);
		// When read from these properties, we should prefer to upload to the revision
		boolean preferCommitDescriptorOverRevision = false;


		// Get branch and timestamp from teamscale.timestamp (TS-38561)
		CommitDescriptor teamscaleTimestampBasedCommitDescriptor = getCommitDescriptorFromTeamscaleTimestampProperty(
				gitProperties,
				entryName, jarFile, dateTimeFormatter);
		if (teamscaleTimestampBasedCommitDescriptor != null) {
			// In this case, as we specifically set this property, we should prefer branch and timestamp to the revision
			preferCommitDescriptorOverRevision = true;
			commitDescriptor = teamscaleTimestampBasedCommitDescriptor;
		}

		if (StringUtils.isEmpty(revision) && commitDescriptor == null) {
			throw new InvalidGitPropertiesException(
					"No entry or invalid value for '" + GIT_PROPERTIES_GIT_COMMIT_ID + "', '" + GIT_PROPERTIES_GIT_COMMIT_ID_FULL +
							"' and " + GIT_PROPERTIES_TEAMSCALE_TIMESTAMP + "'\n" +
							"In " + entryName + " in " + jarFile + "." +
							"\nContents of " + GIT_PROPERTIES_FILE_NAME + ":\n" + gitProperties);
		}

		CommitInfo commitInfo = new CommitInfo(revision, commitDescriptor);
		commitInfo.preferCommitDescriptorOverRevision = preferCommitDescriptorOverRevision;
		return commitInfo;
	}

	private static String getRevisionFromGitProperties(Properties gitProperties) {
		String revision = gitProperties.getProperty(GIT_PROPERTIES_GIT_COMMIT_ID);
		if (StringUtils.isEmpty(revision)) {
			revision = gitProperties.getProperty(GIT_PROPERTIES_GIT_COMMIT_ID_FULL);
		}
		return revision;
	}

	private static CommitDescriptor getCommitDescriptorFromTeamscaleTimestampProperty(Properties gitProperties,
			String entryName, File jarFile,
			DateTimeFormatter dateTimeFormatter) throws InvalidGitPropertiesException {
		String teamscaleTimestampProperty = gitProperties.getProperty(GIT_PROPERTIES_TEAMSCALE_TIMESTAMP);
		if (!StringUtils.isEmpty(teamscaleTimestampProperty)) {
			String[] split = teamscaleTimestampProperty.split(":");
			if (split.length < 2) {
				throw new InvalidGitPropertiesException(
						String.format("%s needs to be in the format <branch>:<timestamp>. You provided %s" +
										"\nIn " + entryName + " in " + jarFile + "." +
										"\nContents of " + GIT_PROPERTIES_FILE_NAME + ":\n" + gitProperties,
								GIT_PROPERTIES_TEAMSCALE_TIMESTAMP, teamscaleTimestampProperty));
			}
			String branch = split[0];
			String timestamp = String.join(":", Arrays.copyOfRange(split, 1, split.length));
			String teamscaleTimestampRegex = "\\d*(?:p\\d*)?";
			Matcher teamscaleTimestampMatcher = Pattern.compile(teamscaleTimestampRegex).matcher(timestamp);
			if (teamscaleTimestampMatcher.matches()) {
				return new CommitDescriptor(branch, timestamp);
			}

			long epochTimestamp = 0;
			try {
				epochTimestamp = ZonedDateTime.parse(timestamp, dateTimeFormatter).toInstant().toEpochMilli();
			} catch (DateTimeParseException e) {
				throw new InvalidGitPropertiesException(
						"Cannot parse commit time '" + timestamp + "' in the '" + GIT_PROPERTIES_TEAMSCALE_TIMESTAMP +
								"' property. It needs to be in the date formats '" + GIT_PROPERTIES_DEFAULT_MAVEN_DATE_FORMAT +
								"' or '" + GIT_PROPERTIES_DEFAULT_GRADLE_DATE_FORMAT + "' or match the Teamscale timestamp format '"
								+ teamscaleTimestampRegex + "'." +
								"\nIn " + entryName + " in " + jarFile + "." +
								"\nContents of " + GIT_PROPERTIES_FILE_NAME + ":\n" + gitProperties, e);
			}

			return new CommitDescriptor(branch, epochTimestamp);
		}
		return null;
	}

	private static CommitDescriptor getCommitDescriptorFromDefaultGitPropertyValues(Properties gitProperties,
			String entryName, File jarFile,
			DateTimeFormatter dateTimeFormatter) throws InvalidGitPropertiesException {
		String gitBranch = gitProperties.getProperty(GIT_PROPERTIES_GIT_BRANCH);
		String gitTime = gitProperties.getProperty(GIT_PROPERTIES_GIT_COMMIT_TIME);
		if (!StringUtils.isEmpty(gitBranch) && !StringUtils.isEmpty(gitTime)) {
			long gitTimestamp = 0;
			try {
				gitTimestamp = ZonedDateTime.parse(gitTime, dateTimeFormatter).toInstant().toEpochMilli();
			} catch (DateTimeParseException e) {
				throw new InvalidGitPropertiesException(
						"Could not parse the timestamp in property '" + GIT_PROPERTIES_GIT_COMMIT_TIME + "'." +
								"\nIn " + entryName + " in " + jarFile + "." +
								"\nContents of " + GIT_PROPERTIES_FILE_NAME + ":\n" + gitProperties, e);
			}
			return new CommitDescriptor(gitBranch, gitTimestamp);
		}
		return null;
	}
}
