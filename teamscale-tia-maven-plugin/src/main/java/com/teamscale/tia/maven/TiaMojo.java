package com.teamscale.tia.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import shadow.org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

@Mojo(name = "tia", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.RUNTIME,
		threadSafe = true)
public class TiaMojo extends AbstractMojo {

	/**
	 * Name of the property used in maven-osgi-test-plugin.
	 */
	private static final String TYCHO_ARG_LINE = "tycho.testArgLine";

	/**
	 * Name of the property used in maven-surefire-plugin.
	 */
	private static final String SUREFIRE_ARG_LINE = "argLine";

	@Parameter(required = true)
	private String teamscaleUrl;

	@Parameter(required = true)
	private String project;

	@Parameter(required = true)
	private String userName;

	@Parameter(property = "teamscale.accessToken", required = true)
	private String accessToken;

	@Parameter
	private String revision;

	@Parameter
	private String endCommit;

	@Parameter
	private String propertyName;

	@Parameter(defaultValue = "Test Impact Analysis")
	private String partition;

	@Parameter(defaultValue = "12888")
	private String agentPort;

	@Parameter
	private String additionalAgentOptions;

	@Parameter(property = "plugin.artifactMap", required = true, readonly = true)
	private Map<String, Artifact> pluginArtifactMap;

	@Parameter(defaultValue = "${project.build.directory}")
	private String projectBuildDir;

	@Parameter(defaultValue = "${session}")
	private MavenSession session;

	private Path targetDirectory;
	private String resolvedEndCommit;

	public void execute() throws MojoExecutionException, MojoFailureException {
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

		Path agentConfigFile = createAgentConfigFile();
		setArgLine(agentConfigFile);
	}

	private void createTargetDirectory() throws MojoFailureException {
		try {
			Files.createDirectories(targetDirectory);
		} catch (IOException e) {
			throw new MojoFailureException("Could not create target directory " + targetDirectory, e);
		}
	}

	private void setArgLine(Path agentConfigFile) {
		String propertyName = getEffectivePropertyName();
		Properties projectProperties = getMavenProject().getProperties();

		String oldValue = projectProperties.getProperty(propertyName);
		String newValue = createAgentOptions(agentConfigFile);
		if (StringUtils.isNotBlank(oldValue)) {
			newValue = newValue + " " + oldValue;
		}

		getLog().info(propertyName + " set to " + newValue);
		projectProperties.setProperty(propertyName, newValue);
	}

	private MavenProject getMavenProject() {
		return session.getCurrentProject();
	}

	private Path createAgentConfigFile() throws MojoFailureException {
		Path configFilePath = targetDirectory.resolve("agent.properties");
		String agentConfig = createAgentConfig();
		try {
			Files.write(configFilePath, Collections.singleton(agentConfig));
		} catch (IOException e) {
			throw new MojoFailureException("Writing the configuration file for the TIA agent failed." +
					" Make sure the path " + configFilePath + " is writeable.", e);
		}

		getLog().info("Agent config file created at " + configFilePath);
		return configFilePath;
	}

	private String createAgentConfig() {
		return "mode=testwise" +
				"\ntia-mode=teamscale-upload" +
				"\nteamscale-server-url=" + teamscaleUrl +
				"\nteamscale-project=" + project +
				"\nteamscale-user=" + userName +
				"\nteamscale-access-token=" + accessToken +
				"\nteamscale-commit=" + resolvedEndCommit +
				"\nteamscale-partition=" + partition +
				"\nhttp-server-port=" + agentPort;
	}

	private String createAgentOptions(Path agentConfigFile) {
		String agentPath = findAgentJarFile().getAbsolutePath();
		String javaagentArgument = "-javaagent:" + agentPath + "=config-file=" + agentConfigFile.toAbsolutePath();
		if (StringUtils.isNotBlank(additionalAgentOptions)) {
			javaagentArgument += "," + additionalAgentOptions;
		}
		return javaagentArgument;
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
