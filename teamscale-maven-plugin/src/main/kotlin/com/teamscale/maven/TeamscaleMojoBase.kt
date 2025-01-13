package com.teamscale.maven

import com.teamscale.maven.GitCommitUtils.getGitHeadRevision
import org.apache.commons.lang3.StringUtils
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import java.io.IOException
import kotlin.io.path.pathString

/**
 * A base class for all Teamscale related maven Mojos. Offers basic attributes and functionality related to Teamscale
 * and Maven.
 */
abstract class TeamscaleMojoBase : AbstractMojo() {
	/**
	 * The URL of the Teamscale instance to which the recorded coverage will be uploaded.
	 */
	@JvmField
	@Parameter
	var teamscaleUrl: String? = null

	/**
	 * The Teamscale project to which the recorded coverage will be uploaded
	 */
	@JvmField
	@Parameter
	var projectId: String? = null

	/**
	 * The username to use to perform the upload. Must have the "Upload external data" permission for the [ ][.projectId]. Can also be specified via the Maven property `teamscale.username`.
	 */
	@JvmField
	@Parameter(property = "teamscale.username")
	var username: String? = null

	/**
	 * Teamscale access token of the [.username]. Can also be specified via the Maven property
	 * `teamscale.accessToken`.
	 */
	@JvmField
	@Parameter(property = "teamscale.accessToken")
	var accessToken: String? = null

	/**
	 * You can optionally use this property to override the code commit to which the coverage will be uploaded. Format:
	 * `BRANCH:UNIX_EPOCH_TIMESTAMP_IN_MILLISECONDS`
	 *
	 *
	 * If no commit and revision is manually specified, the plugin will try to determine the currently checked-out Git
	 * commit. You should specify either commit or revision, not both. If both are specified, a warning is logged and
	 * the revision takes precedence.
	 */
	@Parameter(property = "teamscale.commit")
	var commit: String? = null

	/**
	 * You can optionally use this property to override the revision to which the coverage will be uploaded. If no
	 * commit and revision is manually specified, the plugin will try to determine the current git revision. You should
	 * specify either commit or revision, not both. If both are specified, a warning is logged and the revision takes
	 * precedence.
	 */
	@JvmField
	@Parameter(property = "teamscale.revision")
	var revision: String? = null

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given. Null or
	 * empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	@JvmField
	@Parameter(property = "teamscale.repository")
	var repository: String? = null

	/**
	 * Whether to skip the execution of this Mojo.
	 */
	@JvmField
	@Parameter(defaultValue = "false")
	var skip: Boolean = false

	/**
	 * The running Maven session. Provided automatically by Maven.
	 */
	@JvmField
	@Parameter(defaultValue = "\${session}")
	var session: MavenSession? = null

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
		if (revision.isNullOrBlank()) return
		if (commit.isNullOrBlank()) return
		log.warn("Both revision and commit are set but only one of them is needed. Teamscale will prefer the revision. If that's not intended, please do not set the revision manually.")
	}

	/**
	 * Sets the `resolvedRevision` or `resolvedCommit`. If not provided, try to determine the
	 * revision via the GitCommit class.
	 *
	 * @see GitCommitUtils
	 */
	@Throws(MojoFailureException::class)
	protected fun resolveCommitOrRevision() {
		if (!revision.isNullOrBlank()) {
			resolvedRevision = revision
			return
		}
		if (!commit.isNullOrBlank()) {
			resolvedCommit = commit
			return
		}
		session?.currentProject?.basedir?.toPath()?.let { dir ->
			try {
				resolvedRevision = getGitHeadRevision(dir)
			} catch (e: IOException) {
				throw MojoFailureException(
					"There is no <revision> or <commit> configured in the pom.xml and it was not possible to determine the current revision in ${dir.pathString} from Git", e
				)
			}
		}
	}

	/**
	 * Retrieves the configuration of a goal execution for the given plugin
	 *
	 * @param pluginArtifact The id of the plugin
	 * @param pluginGoal     The name of the goal
	 * @return The configuration DOM if present, otherwise `null`
	 */
	protected fun getExecutionConfigurationDom(
		project: MavenProject,
		pluginArtifact: String?,
		pluginGoal: String?
	): Xpp3Dom? {
		val plugin = project.getPlugin(pluginArtifact) ?: return null

		plugin.executions.forEach { pluginExecution ->
			if (pluginExecution.goals.contains(pluginGoal)) {
				return pluginExecution.configuration as Xpp3Dom
			}
		}

		return null
	}
}
