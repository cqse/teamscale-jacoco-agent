package com.teamscale.maven

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import java.io.IOException

/**
 * A base class for all Teamscale-related Maven Mojos. Offers basic attributes and functionality related to Teamscale
 * and Maven.
 */
abstract class TeamscaleMojoBase : AbstractMojo() {

	/**
	 * The URL of the Teamscale instance to which the recorded coverage will be uploaded.
	 */
	@Parameter
	var teamscaleUrl: String? = null

	/**
	 * The Teamscale project to which the recorded coverage will be uploaded
	 */
	@Parameter
	var projectId: String? = null

	/**
	 * The username to use to perform the upload. Must have the "Upload external data" permission for the [projectId].
	 * Can also be specified via the Maven property `teamscale.username`.
	 */
	@Parameter(property = "teamscale.username")
	lateinit var username: String

	/**
	 * Teamscale access token of the [username]. Can also be specified via the Maven property `teamscale.accessToken`.
	 */
	@Parameter(property = "teamscale.accessToken")
	lateinit var accessToken: String

	/**
	 * You can optionally use this property to override the code commit to which the coverage will be uploaded. Format:
	 * `BRANCH:UNIX_EPOCH_TIMESTAMP_IN_MILLISECONDS`
	 *
	 * If no commit and revision is manually specified, the plugin will try to determine the currently checked-out Git
	 * commit. You should specify either commit or revision, not both. If both are specified, a warning is logged and
	 * the revision takes precedence.
	 */
	@Parameter(property = "teamscale.commit")
	lateinit var commit: String

	/**
	 * You can optionally use this property to override the revision to which the coverage will be uploaded. If no
	 * commit and revision is manually specified, the plugin will try to determine the current git revision. You should
	 * specify either commit or revision, not both. If both are specified, a warning is logged and the revision takes
	 * precedence.
	 */
	@Parameter(property = "teamscale.revision")
	lateinit var revision: String

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given. Null or
	 * empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	@Parameter(property = "teamscale.repository")
	lateinit var repository: String

	/**
	 * Whether to skip the execution of this Mojo.
	 */
	@Parameter(defaultValue = "false")
	var skip: Boolean = false

	/**
	 * The running Maven session. Provided automatically by Maven.
	 */
	@Parameter(defaultValue = "\${session}")
	lateinit var session: MavenSession

	/**
	 * The resolved commit, either provided by the user or determined via the GitCommit class
	 */
	@JvmField
	protected var resolvedCommit: String? = null

	/**
	 * The resolved revision, either provided by the user or determined via the GitCommit class
	 */
	@JvmField
	protected var resolvedRevision: String? = null

	@Throws(MojoExecutionException::class, MojoFailureException::class)
	override fun execute() {
		if (revision.isNotBlank() && commit.isNotBlank()) {
			log.warn(
				"Both revision and commit are set but only one of them is needed. " +
						"Teamscale will prefer the revision. If that's not intended, please do not set the revision manually."
			)
		}
	}

	/**
	 * Sets the `resolvedRevision` or `resolvedCommit`. If not provided, try to determine the
	 * revision via the GitCommit class.
	 *
	 * @see GitCommitUtils
	 */
	@Throws(MojoFailureException::class)
	protected fun resolveCommitOrRevision() {
		when {
			revision.isNotBlank() -> {
				resolvedRevision = revision
			}
			commit.isNotBlank() -> {
				resolvedCommit = commit
			}
			else -> {
				val basedir = session.currentProject.basedir.toPath()
				try {
					resolvedRevision = GitCommitUtils.getGitHeadRevision(basedir)
				} catch (e: IOException) {
					throw MojoFailureException(
						"There is no <revision> or <commit> configured in the pom.xml" +
								" and it was not possible to determine the current revision in $basedir from Git", e
					)
				}
			}
		}
	}

	/**
	 * Retrieves the configuration of a goal execution for the given plugin
	 *
	 * @receiver The maven project
	 * @param pluginArtifact  The id of the plugin
	 * @param pluginGoal      The name of the goal
	 * @return The configuration DOM if present, otherwise `null`
	 */
	protected fun MavenProject.getExecutionConfigurationDom(
		pluginArtifact: String,
		pluginGoal: String
	) = getPlugin(pluginArtifact)
		?.executions
		?.firstOrNull { it.goals.contains(pluginGoal) }
		?.configuration as? Xpp3Dom
}
