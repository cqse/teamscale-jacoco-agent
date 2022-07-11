package com.teamscale.jacoco.agent.upload.artifactory;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocatorUtils;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.InvalidGitPropertiesException;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.jacoco.agent.options.FilePatternResolver;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.collections.Pair;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Config necessary to upload files to an azure file storage. */
public class ArtifactoryConfig {
	/**
	 * Option to specify the artifactory URL. This shall be the entire path down to the directory to which the coverage
	 * should be uploaded to, not only the base url of artifactory.
	 */
	public static final String ARTIFACTORY_URL_OPTION = "artifactory-url";

	/**
	 * Username that shall be used for basic auth. Alternative to basic auth is to use an API key with the
	 * {@link ArtifactoryConfig#ARTIFACTORY_API_KEY_OPTION}
	 */
	public static final String ARTIFACTORY_USER_OPTION = "artifactory-user";

	/**
	 * Password that shall be used for basic auth. Alternative to basic auth is to use an API key with the
	 * {@link ArtifactoryConfig#ARTIFACTORY_API_KEY_OPTION}
	 */
	public static final String ARTIFACTORY_PASSWORD_OPTION = "artifactory-password";

	/**
	 * API key that shall be used to authenticat requests to artifacotry with the
	 * {@link com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryUploader#ARTIFACTORY_API_HEADER}. Alternatively
	 * basic auth with username ({@link ArtifactoryConfig#ARTIFACTORY_USER_OPTION}) and password
	 * ({@link ArtifactoryConfig#ARTIFACTORY_PASSWORD_OPTION}) can be used.
	 */
	public static final String ARTIFACTORY_API_KEY_OPTION = "artifactory-api-key";

	/**
	 * Option that specifies under which path the coverage file shall lie within the zip file that is created for the
	 * upload.
	 */
	public static final String ARTIFACTORY_ZIP_PATH_OPTION = "artifactory-zip-path";

	/**
	 * Specifies the location of the JAR file which includes the git.properties file.
	 */
	public static final String ARTIFACTORY_GIT_PROPERTIES_JAR_OPTION = "artifactory-git-properties-jar";

	/**
	 * Specifies the date format in which the commit timestamp in the git.properties file is formatted.
	 */
	public static final String ARTIFACTORY_GIT_PROPERTIES_COMMIT_DATE_FORMAT_OPTION = "artifactory-git-properties-commit-date-format";

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_USER_OPTION} */
	public HttpUrl url;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_USER_OPTION} */
	public String user;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_PASSWORD_OPTION} */
	public String password;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_ZIP_PATH_OPTION} */
	public String zipPath;

	/** The information regarding a commit. */
	public CommitInfo commitInfo;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_GIT_PROPERTIES_COMMIT_DATE_FORMAT_OPTION} */
	public DateTimeFormatter gitPropertiesCommitTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_API_KEY_OPTION} */
	public String apiKey;

	/**
	 * Handles all command-line options prefixed with 'artifactory-'
	 *
	 * @return true if it has successfully process the given option.
	 */
	public static boolean handleArtifactoryOptions(ArtifactoryConfig options,
												   FilePatternResolver filePatternResolver,
												   String key, String value)
			throws AgentOptionParseException {
		switch (key) {
			case ARTIFACTORY_URL_OPTION:
				options.url = AgentOptionsParser.parseUrl(key, value);
				return true;
			case ARTIFACTORY_USER_OPTION:
				options.user = value;
				return true;
			case ARTIFACTORY_PASSWORD_OPTION:
				options.password = value;
				return true;
			case ARTIFACTORY_ZIP_PATH_OPTION:
				options.zipPath = StringUtils.stripSuffix(value, "/");
				return true;
			case ARTIFACTORY_GIT_PROPERTIES_JAR_OPTION:
				options.commitInfo = ArtifactoryConfig.parseGitProperties(filePatternResolver,
						options.gitPropertiesCommitTimeFormat, key, value);
				return true;
			case ARTIFACTORY_GIT_PROPERTIES_COMMIT_DATE_FORMAT_OPTION:
				options.gitPropertiesCommitTimeFormat = DateTimeFormatter.ofPattern(value);
				return true;
			case ARTIFACTORY_API_KEY_OPTION:
				options.apiKey = value;
				return true;
			default:
				return false;
		}
	}

	/** Checks if all required options are set to upload to artifactory. */
	public boolean hasAllRequiredFieldsSet() {
		boolean requiredAuthOptionsSet = (user != null && password != null) || apiKey != null;
		return url != null && requiredAuthOptionsSet;
	}

	/** Checks if all required fields are null. */
	public boolean hasAllRequiredFieldsNull() {
		return url == null && user == null && password == null && apiKey == null;
	}

	/** Checks whether commit and revision are set. */
	public boolean hasCommitInfo() {
		return commitInfo != null;
	}

	/** Parses the commit information form a git.properties file. */
	public static CommitInfo parseGitProperties(FilePatternResolver filePatternResolver,
												DateTimeFormatter gitPropertiesCommitTimeFormat, String optionName,
												String value) throws AgentOptionParseException {
		File jarFile = filePatternResolver.parsePath(optionName, value).toFile();
		try {
			List<CommitInfo> commitInfo = parseGitProperties(jarFile, gitPropertiesCommitTimeFormat);
			if (commitInfo.isEmpty()) {
				throw new AgentOptionParseException(
						"Found no git.properties files in " + jarFile);
			}
			if (commitInfo.size() > 1) {
				throw new AgentOptionParseException(
						"Found multiple git.properties files in " + jarFile);
			}
			return commitInfo.get(0);
		} catch (IOException | InvalidGitPropertiesException e) {
			throw new AgentOptionParseException("Could not locate a valid git.properties file in " + jarFile, e);
		}
	}

	/** Parses the commit information form a git.properties file. */
	public static List<CommitInfo> parseGitProperties(File jarFile,
													  DateTimeFormatter gitPropertiesCommitTimeFormat) throws IOException, InvalidGitPropertiesException {
		List<Pair<String, Properties>> entriesWithProperties = GitPropertiesLocatorUtils.findGitPropertiesInFile(
				jarFile, true);
		List<CommitInfo> result = new ArrayList<>();

		for (Pair<String, Properties> entryWithProperties : entriesWithProperties) {
			String entry = entryWithProperties.getFirst();
			Properties properties = entryWithProperties.getSecond();

			String revision = GitPropertiesLocatorUtils
					.getGitPropertiesValue(properties, GitPropertiesLocatorUtils.GIT_PROPERTIES_GIT_COMMIT_ID, entry,
							jarFile);
			String branchName = GitPropertiesLocatorUtils
					.getGitPropertiesValue(properties, GitPropertiesLocator.GIT_PROPERTIES_GIT_BRANCH, entry, jarFile);
			long timestamp = ZonedDateTime.parse(GitPropertiesLocatorUtils
							.getGitPropertiesValue(properties, GitPropertiesLocator.GIT_PROPERTIES_GIT_COMMIT_TIME, entry,
									jarFile),
					gitPropertiesCommitTimeFormat).toInstant().toEpochMilli();
			result.add(new CommitInfo(revision, new CommitDescriptor(branchName, timestamp)));

		}
		return result;
	}

	/** Hold information regarding a commit. */
	public static class CommitInfo {
		/** The revision information (git hash). */
		public String revision;

		/** The commit descriptor. */
		public CommitDescriptor commit;

		/** Constructor. */
		public CommitInfo(String revision, CommitDescriptor commit) {
			this.revision = revision;
			this.commit = commit;
		}

		@Override
		public String toString() {
			return commit + "/" + revision;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CommitInfo that = (CommitInfo) o;
			return Objects.equals(revision, that.revision) &&
					Objects.equals(commit, that.commit);
		}

		@Override
		public int hashCode() {
			return Objects.hash(revision, commit);
		}
	}
}
