package com.teamscale.jacoco.agent.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocatorUtils;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.InvalidGitPropertiesException;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.collections.Pair;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;

/** Config necessary to upload files to an azure file storage. */
public class ArtifactoryConfig {
	// TODO documentation?
	public static final String ARTIFACTORY_URL_OPTION = "artifactory-url";
	public static final String ARTIFACTORY_USER_OPTION = "artifactory-user";
	public static final String ARTIFACTORY_PASSWORD_OPTION = "artifactory-password";
	public static final String ARTIFACTORY_ZIP_PATH_OPTION = "artifactory-zip-path";
	public static final String ARTIFACTORY_GIT_PROPERTIES_JAR_OPTION = "artifactory-git-properties-jar";
	public static final String ARTIFACTORY_GIT_PROPERTIES_COMMIT_DATE_FORMAT_OPTION = "artifactory-git-properties-commit-date-format";
	public static final String ARTIFACTORY_API_KEY_OPTION = "artifactory-api-key";
	/** The URL to artifactory */
	public HttpUrl url;

	/** The artifactory username */
	public String user;

	/** The artifactory password */
	public String password;

	/** The path within the zip file, used as partition in Teamscale. */
	public String zipPath;

	/** The information regarding a commit. */
	public CommitInfo commitInfo;

	/** The git time formatter, defaults to git.properties plugin default value. */
	public DateTimeFormatter gitPropertiesCommitTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

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

	/** Checks if none of the required fields is null. */
	public boolean hasAllRequiredFieldsSet() {
		return url != null && user != null && password != null;
	}

	/** Checks if all required fields are null. */
	public boolean hasAllRequiredFieldsNull() {
		return url == null && user == null && password == null;
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
			CommitInfo commitInfo = parseGitProperties(jarFile, gitPropertiesCommitTimeFormat);
			if (commitInfo == null) {
				throw new AgentOptionParseException(
						"Could not locate a git.properties file in " + jarFile.toString());
			}
			return commitInfo;
		} catch (IOException | InvalidGitPropertiesException e) {
			throw new AgentOptionParseException("Could not locate a valid git.properties file in " + jarFile.toString(),
					e);
		}
	}

	/** Parses the commit information form a git.properties file. */
	public static CommitInfo parseGitProperties(File jarFile,
												DateTimeFormatter gitPropertiesCommitTimeFormat) throws IOException, InvalidGitPropertiesException {
		Pair<String, Properties> entryWithProperties = GitPropertiesLocatorUtils.findGitPropertiesInFile(jarFile, true);
		if (entryWithProperties == null) {
			return null;
		}

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
		return new CommitInfo(revision, new CommitDescriptor(branchName, timestamp));
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
