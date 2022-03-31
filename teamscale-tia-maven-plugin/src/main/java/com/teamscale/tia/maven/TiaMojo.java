package com.teamscale.tia.maven;

import shadow.org.apache.commons.lang3.StringUtils;
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

import java.io.File;
import java.io.IOException;
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

	@Parameter(defaultValue = "${session}")
	private MavenSession session;

	@Parameter(property = "project", readonly = true)
	private MavenProject mavenProject;

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

	@Parameter(property = "plugin.artifactMap", required = true, readonly = true)
	Map<String, Artifact> pluginArtifactMap;

	public void execute() throws MojoExecutionException, MojoFailureException {
		setTiaProperty("reportDirectory", "./target/tia");
		setTiaProperty("server.url", teamscaleUrl);
		setTiaProperty("server.project", project);
		setTiaProperty("server.userName", userName);
		setTiaProperty("server.userAccessToken", accessToken);
		setTiaProperty("endCommit", getEndCommit());

		setArgLine();
	}

	private void setArgLine() {
		String propertyName = getEffectivePropertyName();
		Properties projectProperties = mavenProject.getProperties();
		String oldValue = projectProperties.getProperty(propertyName);
		String newValue = createAgentOptions(oldValue);
		getLog().info(propertyName + " set to " + newValue);
		projectProperties.setProperty(propertyName, newValue);
	}

	private String createAgentOptions(String oldValue) {
		String agentPath = findAgentJarFile().getAbsolutePath();

		String javaAgentOption = "-javaagent:" + agentPath + "=config-file=/home/k/proj/tia-maven/agent.properties";
		if (StringUtils.isNotBlank(oldValue)) {
			javaAgentOption = javaAgentOption + " " + oldValue;
		}
		return javaAgentOption;
	}

	private File findAgentJarFile() {
		Artifact agentArtifact = pluginArtifactMap.get("com.teamscale:teamscale-jacoco-agent");
		return agentArtifact.getFile();
	}

	private String getEffectivePropertyName() {
		if (StringUtils.isNotBlank(propertyName)) {
			return propertyName;
		}
		if ("eclipse-test-plugin".equals(mavenProject.getPackaging())) {
			return TYCHO_ARG_LINE;
		}
		return SUREFIRE_ARG_LINE;
	}

	private String getEndCommit() throws MojoFailureException {
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
