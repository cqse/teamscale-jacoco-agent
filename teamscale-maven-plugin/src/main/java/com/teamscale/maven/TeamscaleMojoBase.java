package com.teamscale.maven;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A base class for all Teamscale related maven Mojos.
 * Offers basic attributes and functionality related to Teamscale and Maven.
 */
public abstract class TeamscaleMojoBase extends AbstractMojo {

	/**
	 * The URL of the Teamscale instance to which the recorded coverage will be uploaded.
	 */
	@Parameter()
	public String teamscaleUrl;

	/**
	 * The Teamscale project to which the recorded coverage will be uploaded
	 */
	@Parameter()
	public String projectId;

	/**
	 * The username to use to perform the upload. Must have the "Upload external data" permission for the {@link
	 * #projectId}. Can also be specified via the Maven property {@code teamscale.username}.
	 */
	@Parameter(property = "teamscale.username")
	public String username;

	/**
	 * Teamscale access token of the {@link #username}. Can also be specified via the Maven property {@code
	 * teamscale.accessToken}.
	 */
	@Parameter(property = "teamscale.accessToken")
	public String accessToken;

	/**
	 * You can optionally use this property to override the code commit to which the coverage will be uploaded. Format:
	 * {@code BRANCH:UNIX_EPOCH_TIMESTAMP_IN_MILLISECONDS}
	 * <p>
	 * If no end commit is manually specified, the plugin will try to determine the currently checked out Git commit.
	 */
	@Parameter(property = "teamscale.commit")
	public String commit;

	/**
	 * You can optionally use this property to override the revision to which the coverage will be uploaded.
	 * If no revision is manually specified, the plugin will try to determine the current git revision.
	 */
	@Parameter(property = "teamscale.revision")
	public String revision;

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	@Parameter(property = "teamscale.repository")
	public String repository;

	/**
	 * Whether to skip the execution of this Mojo.
	 */
	@Parameter(defaultValue = "false")
	public boolean skip;

	/**
	 * The running Maven session. Provided automatically by Maven.
	 */
	@Parameter(defaultValue = "${session}")
	public MavenSession session;

	/**
	 * The resolved commit, either provided by the user or determined via the GitCommit class
	 */
	protected String resolvedCommit;

	/**
	 * The resolved revision, either provided by the user or determined via the GitCommit class
	 */
	protected String resolvedRevision;

	/**
	 * Sets the <code>resolvedCommit</code> and <code>resolvedRevision</code>, if not provided, via the GitCommit class
	 * @see GitCommit
	 */
	protected void resolveCommit() throws MojoFailureException {
		if (StringUtils.isNotBlank(commit)) {
			resolvedCommit = commit;
			return;
		}
		Path basedir = session.getCurrentProject().getBasedir().toPath();
		try {
			GitCommit commit = GitCommit.getGitHeadCommitDescriptor(basedir);
			resolvedCommit = commit.branch + ":" + commit.timestamp;
		} catch (IOException e) {
			throw new MojoFailureException("There is no <commit> configured in the pom.xml" +
					" and it was not possible to determine the checked out commit in " + basedir + " from Git", e);
		}
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (StringUtils.isNotEmpty(revision) && StringUtils.isNotBlank(commit)) {
			getLog().warn("Both revision and commit are set but only one of them is needed. " +
					"Teamscale will prefer the revision. If that's not intended, please do not set the revision manually.");
		}
	}

	/**
	 * Sets the <code>resolvedRevision</code>, if not provided, via the GitCommit class
	 *
	 * @see GitCommit
	 */
	protected void resolveRevision() throws MojoFailureException {
		if (StringUtils.isNotBlank(revision)) {
			resolvedRevision = revision;
		} else {
			Path basedir = session.getCurrentProject().getBasedir().toPath();
			try {
				GitCommit commit = GitCommit.getGitHeadCommitDescriptor(basedir);
				resolvedRevision = commit.sha1;
			} catch (IOException e) {
				throw new MojoFailureException("There is no <revision> configured in the pom.xml" +
						" and it was not possible to determine the current revision in " + basedir + " from Git", e);
			}
		}
	}

	/**
	 * Retrieves the configuration of a goal execution for the given plugin
	 * @param pluginArtifact The id of the plugin
	 * @param pluginGoal The name of the goal
	 * @return The configuration DOM if present, otherwise <code>null</code>
	 */
	protected Xpp3Dom getExecutionConfigurationDom(MavenProject project, String pluginArtifact, String pluginGoal) {
		Plugin plugin = project.getPlugin(pluginArtifact);
		if (plugin == null) {
			return null;
		}

		for (PluginExecution pluginExecution : plugin.getExecutions()) {
			if (pluginExecution.getGoals().contains(pluginGoal)) {
				return (Xpp3Dom) pluginExecution.getConfiguration();
			}
		}

		return null;
	}
}
