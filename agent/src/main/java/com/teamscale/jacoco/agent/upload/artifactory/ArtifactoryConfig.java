package com.teamscale.jacoco.agent.upload.artifactory;

import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.CommitInfo;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocatorUtils;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.InvalidGitPropertiesException;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.jacoco.agent.upload.UploaderException;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
	 * API key that shall be used to authenticate requests to artifactory with the
	 * {@link com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryUploader#ARTIFACTORY_API_HEADER}. Alternatively
	 * basic auth with username ({@link ArtifactoryConfig#ARTIFACTORY_USER_OPTION}) and password
	 * ({@link ArtifactoryConfig#ARTIFACTORY_PASSWORD_OPTION}) can be used.
	 */
	public static final String ARTIFACTORY_API_KEY_OPTION = "artifactory-api-key";

	/**
	 * Option that specifies if the legacy path for uploading files to artifactory should be used instead of the new
	 * standard path.
	 */
	public static final String ARTIFACTORY_LEGACY_PATH_OPTION = "artifactory-legacy-path";

	/**
	 * Option that specifies under which path the coverage file shall lie within the zip file that is created for the
	 * upload.
	 */
	public static final String ARTIFACTORY_ZIP_PATH_OPTION = "artifactory-zip-path";

	/**
	 * Option that specifies intermediate directories which should be appended.
	 */
	public static final String ARTIFACTORY_PATH_SUFFIX = "artifactory-path-suffix";

	/**
	 * Specifies the location of the JAR file which includes the git.properties file.
	 */
	public static final String ARTIFACTORY_GIT_PROPERTIES_JAR_OPTION = "artifactory-git-properties-jar";

	/**
	 * Specifies the date format in which the commit timestamp in the git.properties file is formatted.
	 */
	public static final String ARTIFACTORY_GIT_PROPERTIES_COMMIT_DATE_FORMAT_OPTION = "artifactory-git-properties-commit-date-format";

	/**
	 * Specifies the partition for which the upload is.
	 */
	public static final String ARTIFACTORY_PARTITION = "artifactory-partition";

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_USER_OPTION} */
	public HttpUrl url;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_USER_OPTION} */
	public String user;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_PASSWORD_OPTION} */
	public String password;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_LEGACY_PATH_OPTION} */
	public boolean legacyPath = false;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_ZIP_PATH_OPTION} */
	public String zipPath;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_PATH_SUFFIX} */
	public String pathSuffix;

	/** The information regarding a commit. */
	public CommitInfo commitInfo;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_API_KEY_OPTION} */
	public String apiKey;

	/** Related to {@link ArtifactoryConfig#ARTIFACTORY_PARTITION} */
	public String partition;

	/**
	 * Handles all command-line options prefixed with 'artifactory-'
	 *
	 * @return true if it has successfully processed the given option.
	 */
	public static boolean handleArtifactoryOptions(ArtifactoryConfig options, String key, String value) throws AgentOptionParseException {
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
			case ARTIFACTORY_LEGACY_PATH_OPTION:
				options.legacyPath = Boolean.parseBoolean(value);
				return true;
			case ARTIFACTORY_ZIP_PATH_OPTION:
				options.zipPath = StringUtils.stripSuffix(value, "/");
				return true;
			case ARTIFACTORY_PATH_SUFFIX:
				options.pathSuffix = StringUtils.stripSuffix(value, "/");
				return true;
			case ARTIFACTORY_API_KEY_OPTION:
				options.apiKey = value;
				return true;
			case ARTIFACTORY_PARTITION:
				options.partition = value;
				return true;
			default:
				return false;
		}
	}

	/** Checks if all required options are set to upload to artifactory. */
	public boolean hasAllRequiredFieldsSet() {
		boolean requiredAuthOptionsSet = (user != null && password != null) || apiKey != null;
		boolean partitionSet = partition != null || legacyPath;
		return url != null && partitionSet && requiredAuthOptionsSet;
	}

	/** Checks if all required fields are null. */
	public boolean hasAllRequiredFieldsNull() {
		return url == null && user == null && password == null && apiKey == null && partition == null;
	}

	/** Checks whether commit and revision are set. */
	public boolean hasCommitInfo() {
		return commitInfo != null;
	}

	/** Parses the commit information form a git.properties file. */
	public static CommitInfo parseGitProperties(
			File jarFile, boolean searchRecursively, @Nullable DateTimeFormatter gitPropertiesCommitTimeFormat)
			throws UploaderException {
		try {
			List<CommitInfo> commitInfo = GitPropertiesLocatorUtils.getCommitInfoFromGitProperties(jarFile, true, searchRecursively, gitPropertiesCommitTimeFormat);
			if (commitInfo.isEmpty()) {
				throw new UploaderException("Found no git.properties files in " + jarFile);
			}
			if (commitInfo.size() > 1) {
				throw new UploaderException("Found multiple git.properties files in " + jarFile
						+ ". Uploading to multiple projects is currently not possible with Artifactory. "
						+ "Please contact CQSE if you need this feature.");
			}
			return commitInfo.get(0);
		} catch (IOException | InvalidGitPropertiesException e) {
			throw new UploaderException("Could not locate a valid git.properties file in " + jarFile, e);
		}
	}
}
