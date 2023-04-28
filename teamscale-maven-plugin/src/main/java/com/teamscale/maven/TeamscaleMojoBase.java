package com.teamscale.maven;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * A base class for all Teamscale related maven Mojos.
 * Offers basic attributes and functionality related to Teamscale and Maven.
 */
public abstract class TeamscaleMojoBase extends AbstractMojo {

	/**
	 * The URL of the Teamscale instance to which the recorded coverage will be uploaded.
	 */
	@Parameter(required = true)
	public String teamscaleUrl;

	/**
	 * The Teamscale project to which the recorded coverage will be uploaded
	 */
	@Parameter(required = true)
	public String projectId;

	/**
	 * The username to use to perform the upload. Must have the "Upload external data" permission for the {@link
	 * #projectId}. Can also be specified via the Maven property {@code teamscale.username}.
	 */
	@Parameter(property = "teamscale.username", required = true)
	public String username;

	/**
	 * Teamscale access token of the {@link #username}. Can also be specified via the Maven property {@code
	 * teamscale.accessToken}.
	 */
	@Parameter(property = "teamscale.accessToken", required = true)
	public String accessToken;

	/**
	 * You can optionally use this property to override the code commit to which the coverage will be uploaded. Format:
	 * {@code BRANCH:UNIX_EPOCH_TIMESTAMP_IN_MILLISECONDS}
	 * <p>
	 * If no end commit is manually specified, the plugin will try to determine the currently checked out Git commit.
	 */
	@Parameter
	public String endCommit;

	/**
	 * You can optionally use this property to override the revision to which the coverage will be uploaded.
	 * If no revision is manually specified, the plugin will try to determine the current git revision.
	 */
	@Parameter
	public String revision;

	/**
	 * Whether to skip the execution of this Mojo.
	 */
	@Parameter(defaultValue = "false")
	public boolean skip;

	/**
	 * Map of resolved Maven artifacts. Provided automatically by Maven.
	 */
	@Parameter(property = "plugin.artifactMap", required = true, readonly = true)
	public Map<String, Artifact> pluginArtifactMap;

	/**
	 * The project build directory (usually: {@code ./target}). Provided automatically by Maven.
	 */
	@Parameter(defaultValue = "${project.build.directory}")
	public String projectBuildDir;

	/**
	 * The running Maven session. Provided automatically by Maven.
	 */
	@Parameter(defaultValue = "${session}")
	public MavenSession session;

	protected String resolvedEndCommit;

	protected String resolvedRevision;

	protected void resolveEndCommit() throws MojoFailureException {
		Path basedir = session.getCurrentProject().getBasedir().toPath();
		try {
			GitCommit commit = GitCommit.getGitHeadCommitDescriptor(basedir);
			if (StringUtils.isNotBlank(endCommit)) {
				resolvedEndCommit = commit.branch + ":" + commit.timestamp;
			}
			if (StringUtils.isNotBlank(revision)) {
				resolvedRevision = commit.sha1;
			}
		} catch (IOException e) {
			throw new MojoFailureException("You did not configure an <endCommit> in the pom.xml" +
					" and I could also not determine the checked out commit in " + basedir + " from Git", e);
		}
	}

	@Nullable
	protected Xpp3Dom getConfigurationDom(String pluginArtifact) {
		Map<String, Plugin> plugins = session.getCurrentProject().getModel().getBuild().getPluginsAsMap();
		Plugin plugin = plugins.get(pluginArtifact);
		if (plugin == null) {
			return null;
		}

		return (Xpp3Dom) plugin.getConfiguration();
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
