package com.teamscale.tia.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import shadow.org.apache.commons.compress.utils.IOUtils;
import shadow.org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public abstract class TiaMojoBase extends AbstractMojo {

	/**
	 * Name of the property used in maven-osgi-test-plugin.
	 */
	private static final String TYCHO_ARG_LINE = "tycho.testArgLine";

	/**
	 * Name of the property used in maven-surefire-plugin.
	 */
	private static final String SUREFIRE_ARG_LINE = "argLine";

	@Parameter(required = true)
	public String teamscaleUrl;

	@Parameter(required = true)
	public String project;

	@Parameter(required = true)
	public String userName;

	@Parameter(property = "teamscale.accessToken", required = true)
	public String accessToken;

	// TODO (FS) should we also allow specifying a revision?
	@Parameter
	public String endCommit;

	@Parameter
	public String includes;

	@Parameter
	public String excludes;

	@Parameter
	public String propertyName;

	@Parameter(defaultValue = "12888")
	public String agentPort;

	@Parameter
	public String additionalAgentOptions;

	@Parameter(defaultValue = "INFO")
	public String agentLogLevel;

	@Parameter(property = "plugin.artifactMap", required = true, readonly = true)
	public Map<String, Artifact> pluginArtifactMap;

	@Parameter(defaultValue = "${project.build.directory}")
	public String projectBuildDir;

	@Parameter(defaultValue = "${session}")
	public MavenSession session;

	private Path targetDirectory;
	private String resolvedEndCommit;

	public void execute() throws MojoFailureException {
		targetDirectory = Paths.get(projectBuildDir, "tia").toAbsolutePath();
		createTargetDirectory();

		resolvedEndCommit = resolveEndCommit();

		setTiaProperty("reportDirectory", targetDirectory.toString());
		setTiaProperty("server.url", teamscaleUrl);
		setTiaProperty("server.project", project);
		setTiaProperty("server.userName", userName);
		setTiaProperty("server.userAccessToken", accessToken);
		setTiaProperty("endCommit", resolvedEndCommit);
		setTiaProperty("agentsUrls", "http://localhost:" + agentPort);

		Path agentConfigFile = createAgentConfigFiles();
		Path logFilePath = targetDirectory.resolve("agent.log");
		setArgLine(agentConfigFile, logFilePath);
	}

	/**
	 * @return the partition to upload testwise coverage to.
	 */
	protected abstract String getPartition();

	private void createTargetDirectory() throws MojoFailureException {
		try {
			Files.createDirectories(targetDirectory);
		} catch (IOException e) {
			throw new MojoFailureException("Could not create target directory " + targetDirectory, e);
		}
	}

	private void setArgLine(Path agentConfigFile, Path logFilePath) {
		String propertyName = getEffectivePropertyName();
		Properties projectProperties = getMavenProject().getProperties();

		String oldValue = removePreviousTiaAgent(projectProperties.getProperty(propertyName));
		String newValue = createJvmOptions(agentConfigFile, logFilePath);
		if (StringUtils.isNotBlank(oldValue)) {
			newValue = newValue + " " + oldValue;
		}

		getLog().info(propertyName + " set to " + newValue);
		projectProperties.setProperty(propertyName, newValue);
	}

	private String removePreviousTiaAgent(String argLine) {
		if (argLine == null) {
			return null;
		}
		return argLine.replaceAll("-Dteamscale.markstart.*teamscale.markend", "");
	}

	private MavenProject getMavenProject() {
		return session.getCurrentProject();
	}

	private Path createAgentConfigFiles() throws MojoFailureException {
		Path loggingConfigPath = targetDirectory.resolve("logback.xml");
		try (OutputStream loggingConfigOutputStream = Files.newOutputStream(loggingConfigPath)) {
			IOUtils.copy(readAgentLogbackConfig(), loggingConfigOutputStream);
		} catch (IOException e) {
			throw new MojoFailureException("Writing the logging configuration file for the TIA agent failed." +
					" Make sure the path " + loggingConfigPath + " is writeable.", e);
		}

		Path configFilePath = targetDirectory.resolve("agent.properties");
		String agentConfig = createAgentConfig(loggingConfigPath);
		try {
			Files.write(configFilePath, Collections.singleton(agentConfig));
		} catch (IOException e) {
			throw new MojoFailureException("Writing the configuration file for the TIA agent failed." +
					" Make sure the path " + configFilePath + " is writeable.", e);
		}

		getLog().info("Agent config file created at " + configFilePath);
		return configFilePath;
	}

	private InputStream readAgentLogbackConfig() {
		return TiaMojoBase.class.getResourceAsStream("logback-agent.xml");
	}

	private String createAgentConfig(Path loggingConfigPath) {
		String config = "mode=testwise" +
				"\ntia-mode=teamscale-upload" +
				"\nteamscale-server-url=" + teamscaleUrl +
				"\nteamscale-project=" + project +
				"\nteamscale-user=" + userName +
				"\nteamscale-access-token=" + accessToken +
				"\nteamscale-commit=" + resolvedEndCommit +
				"\nteamscale-partition=" + getPartition() +
				"\nhttp-server-port=" + agentPort +
				"\nlogging-config=" + loggingConfigPath;
		if (StringUtils.isNotBlank(includes)) {
			config += "\nincludes=" + includes;
		}
		if (StringUtils.isNotBlank(excludes)) {
			config += "\nexcludes=" + excludes;
		}
		return config;
	}

	private String createJvmOptions(Path agentConfigFile, Path logFilePath) {
		String agentPath = findAgentJarFile().getAbsolutePath();
		String javaagentArgument = "-Dteamscale.markstart '-javaagent:" + agentPath + "=config-file=" + agentConfigFile.toAbsolutePath();
		if (StringUtils.isNotBlank(additionalAgentOptions)) {
			javaagentArgument += "," + additionalAgentOptions;
		}
		javaagentArgument += "'";
		return javaagentArgument + " '-DTEAMSCALE_AGENT_LOG_FILE=" + logFilePath + "'" +
				" -DTEAMSCALE_AGENT_LOG_LEVEL=" + agentLogLevel + " -Dteamscale.markend";
	}

	private File findAgentJarFile() {
		Artifact agentArtifact = pluginArtifactMap.get("com.teamscale:teamscale-jacoco-agent");
		return agentArtifact.getFile();
	}

	private String getEffectivePropertyName() {
		if (StringUtils.isNotBlank(propertyName)) {
			return propertyName;
		}
		if ("eclipse-test-plugin".equals(getMavenProject().getPackaging())) {
			return TYCHO_ARG_LINE;
		}
		return SUREFIRE_ARG_LINE;
	}

	private String resolveEndCommit() throws MojoFailureException {
		if (StringUtils.isNotBlank(endCommit)) {
			return endCommit;
		}

		try {
			GitCommit commit = getGitHeadCommitDescriptor(session.getCurrentProject().getBasedir());
			return commit.branch + ":" + commit.timestamp;
		} catch (IOException e) {
			throw new MojoFailureException(
					"You did not configure an <endCommit> in the pom.xml and I could also not determine the checked out commit from Git",
					e);
		}
	}

	private static class GitCommit {
		public final String ref;
		public final long timestamp;
		public final String branch;

		private GitCommit(String ref, long timestamp, String branch) {
			this.ref = ref;
			this.timestamp = timestamp;
			this.branch = branch;
		}
	}

	private static GitCommit getGitHeadCommitDescriptor(File baseDirectory) throws IOException {
		Git git = Git.open(baseDirectory);
		Repository repository = git.getRepository();
		String branch = repository.getBranch();
		RevCommit commit = getCommit(repository, branch);
		long commitTimeSeconds = commit.getCommitTime();
		String ref = repository.getRefDatabase().findRef("HEAD").getObjectId().getName();
		return new GitCommit(ref, commitTimeSeconds * 1000L, branch);
	}

	private static RevCommit getCommit(Repository repository, String revisionBranchOrTag) throws IOException {
		try (RevWalk revWalk = new RevWalk(repository)) {
			Ref head = repository.getRefDatabase().findRef(revisionBranchOrTag);
			if (head != null) {
				return revWalk.parseCommit(head.getLeaf().getObjectId());
			} else {
				return revWalk.parseCommit(ObjectId.fromString(revisionBranchOrTag));
			}
		}
	}

	/**
	 * Sets a user property in the TIA namespace. User properties are respected both during the build and during tests
	 * (as e.g. failsafe tests are often run in a separate JVM spawned by Maven).
	 */
	private void setTiaProperty(String name, String value) {
		String fullyQualifiedName = "teamscale.test.impacted." + name;
		if (session.getUserProperties().get(fullyQualifiedName) == null) {
			session.getUserProperties().setProperty(fullyQualifiedName, value);
		}
	}
}
