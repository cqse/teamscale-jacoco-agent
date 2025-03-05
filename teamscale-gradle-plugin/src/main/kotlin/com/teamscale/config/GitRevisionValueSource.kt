package com.teamscale.config

import org.eclipse.jgit.api.Git
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.util.logging.Logger

/** Provider that tries to determine the repository revision either from the environment variables or from a checked-out Git repository. */
abstract class GitRevisionValueSource : ValueSource<String, GitRevisionValueSource.Parameters> {
	interface Parameters : ValueSourceParameters {
		val projectDirectory: DirectoryProperty
	}

	companion object {
		private val LOGGER = Logger.getLogger("GitRevisionValueSource")
	}

	override fun obtain(): String? {
		EnvironmentVariableChecker.findCommit()?.let {
			return it
		}

		try {
			val git = Git.open(parameters.projectDirectory.get().asFile)
			return git.repository.refDatabase.findRef("HEAD").objectId.name
		} catch (e: Exception) {
			LOGGER.info { "Failed to auto-detect git revision from checked out repository! " + e.stackTraceToString()}
			return null
		}
	}
}

/**
 * Checks well-known environment variables for commit infos.
 */
private object EnvironmentVariableChecker {

	private val logger = Logger.getLogger("EnvironmentVariableChecker")

	private val COMMIT_ENVIRONMENT_VARIABLES: List<String> = mutableListOf( // user-specified as a fallback
		"COMMIT",  // Git
		"GIT_COMMIT",  // Jenkins
		// https://www.theserverside.com/blog/Coffee-Talk-Java-News-Stories-and-Opinions/Complete-Jenkins-Git-environment-variables-list-for-batch-jobs-and-shell-script-builds
		"Build.SourceVersion",  // Azure DevOps
		// https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops&tabs=yaml#build-variables
		"CIRCLE_SHA1",  // Circle CI
		// https://circleci.com/docs/2.0/env-vars/#built-in-environment-variables
		"TRAVIS_COMMIT",  // Travis CI
		// https://docs.travis-ci.com/user/environment-variables/#default-environment-variables
		"BITBUCKET_COMMIT",  // Bitbucket Pipelines
		// https://confluence.atlassian.com/bitbucket/environment-variables-794502608.html
		"CI_COMMIT_SHA",  // GitLab Pipelines
		// https://docs.gitlab.com/ee/ci/variables/predefined_variables.html
		"APPVEYOR_REPO_COMMIT",  // AppVeyor https://www.appveyor.com/docs/environment-variables/
		"GITHUB_SHA",  // GitHub actions
		// https://help.github.com/en/actions/configuring-and-managing-workflows/using-environment-variables#default-environment-variables
		// SVN
		"SVN_REVISION",  // Jenkins
		// https://stackoverflow.com/questions/43780145/no-svn-revision-in-jenkins-environment-variable
		// https://issues.jenkins-ci.org/browse/JENKINS-14797
		// Both
		"build_vcs_number" // TeamCity
		// https://confluence.jetbrains.com/display/TCD8/Predefined+Build+Parameters
		// https://stackoverflow.com/questions/2882953/how-to-get-branch-specific-svn-revision-numbers-in-teamcity
	)

	/**
	 * Returns either a commit that was found in an environment variable (Git SHA1
	 * or SVN revision number or TFS changeset number) or null if none was found.
	 */
	fun findCommit(): String? {
		for (variable in COMMIT_ENVIRONMENT_VARIABLES) {
			val commit = System.getenv(variable)
			if (commit != null) {
				logger.fine("Using commit/revision/changeset $commit from environment variable $variable")
				return commit
			}
		}

		logger.fine("Found no commit/revision/changeset info in any environment variables.")
		return null
	}
}
