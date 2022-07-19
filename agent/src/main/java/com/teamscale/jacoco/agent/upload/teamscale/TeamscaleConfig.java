package com.teamscale.jacoco.agent.upload.teamscale;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.StringUtils;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocatorUtils;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.InvalidGitPropertiesException;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.jacoco.agent.options.FilePatternResolver;
import com.teamscale.report.util.BashFileSkippingInputStream;
import com.teamscale.report.util.ILogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/** Config necessary for direct Teamscale upload. */
public class TeamscaleConfig {

	/** Option name that allows to specify to which branch coverage should be uploaded to (branch:timestamp). */
	public static final String TEAMSCALE_COMMIT_OPTION = "teamscale-commit";

	/** Option name that allows to specify a git commit hash to which coverage should be uploaded to. */
	public static final String TEAMSCALE_REVISION_OPTION = "teamscale-revision";

	/** Option name that allows to specify a jar file that contains the git commit hash in a MANIFEST.MF file. */
	public static final String TEAMSCALE_REVISION_MANIFEST_JAR_OPTION = "teamscale-revision-manifest-jar";

	/** Option name that allows to specify a jar file that contains the branch name and timestamp in a MANIFEST.MF file. */
	public static final String TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION = "teamscale-commit-manifest-jar";

	/** Option name that allows to specify a jar file that contains the git commit hash in a git.properties file. */
	public static final String TEAMSCALE_GIT_PROPERTIES_JAR_OPTION = "teamscale-git-properties-jar";

	private final ILogger logger;
	private final FilePatternResolver filePatternResolver;

	public TeamscaleConfig(ILogger logger, FilePatternResolver filePatternResolver) {
		this.logger = logger;
		this.filePatternResolver = filePatternResolver;
	}

	/**
	 * Handles all command line options prefixed with "teamscale-".
	 *
	 * @return true if it has successfully processed the given option.
	 */
	public boolean handleTeamscaleOptions(TeamscaleServer teamscaleServer,
										  String key, String value)
			throws AgentOptionParseException {
		switch (key) {
			case "teamscale-server-url":
				teamscaleServer.url = AgentOptionsParser.parseUrl(key, value);
				return true;
			case "teamscale-project":
				teamscaleServer.project = value;
				return true;
			case "teamscale-user":
				teamscaleServer.userName = value;
				return true;
			case "teamscale-access-token":
				teamscaleServer.userAccessToken = value;
				return true;
			case "teamscale-partition":
				teamscaleServer.partition = value;
				return true;
			case TEAMSCALE_COMMIT_OPTION:
				teamscaleServer.commit = parseCommit(value);
				return true;
			case TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION:
				teamscaleServer.commit = getCommitFromManifest(
						filePatternResolver.parsePath(key, value).toFile());
				return true;
			case TEAMSCALE_GIT_PROPERTIES_JAR_OPTION:
				teamscaleServer.revision = getRevisionFromGitProperties(key, value);
				return true;
			case "teamscale-message":
				teamscaleServer.setMessage(value);
				return true;
			case TEAMSCALE_REVISION_OPTION:
				teamscaleServer.revision = value;
				return true;
			case TEAMSCALE_REVISION_MANIFEST_JAR_OPTION:
				teamscaleServer.revision = getRevisionFromManifest(
						filePatternResolver.parsePath(key, value).toFile());
				return true;
			default:
				return false;
		}
	}

	/**
	 * Parses the the string representation of a commit to a {@link CommitDescriptor} object.
	 * <p>
	 * The expected format is "branch:timestamp".
	 */
	private CommitDescriptor parseCommit(String commit) throws AgentOptionParseException {
		String[] split = commit.split(":");
		if (split.length != 2) {
			throw new AgentOptionParseException("Invalid commit given " + commit);
		}
		return new CommitDescriptor(split[0], split[1]);
	}

	/**
	 * Reads `Branch` and `Timestamp` entries from the given jar/war file's manifest and builds a commit descriptor out
	 * of it.
	 */
	private CommitDescriptor getCommitFromManifest(File jarFile) throws AgentOptionParseException {
		Manifest manifest = getManifestFromJarFile(jarFile);
		String branch = manifest.getMainAttributes().getValue("Branch");
		String timestamp = manifest.getMainAttributes().getValue("Timestamp");
		if (StringUtils.isEmpty(branch)) {
			throw new AgentOptionParseException("No entry 'Branch' in MANIFEST");
		} else if (StringUtils.isEmpty(timestamp)) {
			throw new AgentOptionParseException("No entry 'Timestamp' in MANIFEST");
		}
		logger.debug("Found commit " + branch + ":" + timestamp + " in file " + jarFile);
		return new CommitDescriptor(branch, timestamp);
	}

	/**
	 * Reads `Git_Commit` entry from the given jar/war file's manifest and sets it as revision.
	 */
	private String getRevisionFromManifest(File jarFile) throws AgentOptionParseException {
		Manifest manifest = getManifestFromJarFile(jarFile);
		String revision = manifest.getMainAttributes().getValue("Revision");
		if (StringUtils.isEmpty(revision)) {
			// currently needed option for a customer
			if (manifest.getAttributes("Git") != null) {
				revision = manifest.getAttributes("Git").getValue("Git_Commit");
			}

			if (StringUtils.isEmpty(revision)) {
				throw new AgentOptionParseException("No entry 'Revision' in MANIFEST");
			}
		}
		logger.debug("Found revision " + revision + " in file " + jarFile);
		return revision;
	}

	/**
	 * Reads the JarFile to extract the MANIFEST.MF.
	 */
	private Manifest getManifestFromJarFile(File jarFile) throws AgentOptionParseException {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(new FileInputStream(jarFile)))) {
			Manifest manifest = jarStream.getManifest();
			if (manifest == null) {
				throw new AgentOptionParseException(
						"Unable to read manifest from " + jarFile + ". Maybe the manifest is corrupt?");
			}
			return manifest;
		} catch (IOException e) {
			throw new AgentOptionParseException("Reading jar " + jarFile.getAbsolutePath() + " for obtaining commit "
					+ "descriptor from MANIFEST failed", e);
		}
	}

	private String getRevisionFromGitProperties(String optionName,
												String value) throws AgentOptionParseException {
		File jarFile = filePatternResolver.parsePath(optionName, value).toFile();
		try {
			List<String> revisions = GitPropertiesLocatorUtils.getRevisionsFromGitProperties(jarFile, true);
			if (revisions.isEmpty()) {
				throw new AgentOptionParseException("Found no git.properties files in " + jarFile);
			}
			if (revisions.size() > 1) {
				throw new AgentOptionParseException("Found multiple git.properties files in " + jarFile +
						". Uploading to multiple projects is currently not possible with the option teamscale-git-properties-jar. " +
						"Please contact CQSE if you need this feature.");
			}
			return revisions.get(0);
		} catch (IOException | InvalidGitPropertiesException e) {
			throw new AgentOptionParseException("Could not locate a valid git.properties file in " + jarFile, e);
		}
	}
}
