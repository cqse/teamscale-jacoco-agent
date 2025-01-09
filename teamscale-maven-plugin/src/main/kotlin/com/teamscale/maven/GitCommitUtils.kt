package com.teamscale.maven

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object GitCommitUtils {
	private const val HEAD_REF = "HEAD"

	/**
	 * Determines the current HEAD commit in the Git repository located in the or above the given search directory.
	 *
	 * @throws IOException if reading from the Git repository fails or the current directory is not a Git repository.
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun getGitHeadRevision(searchDirectory: Path): String {
		val gitDirectory = findGitDirectory(searchDirectory)
			?: throw IOException("Could not find git directory in $searchDirectory")
		return getHeadCommitFromGitDirectory(gitDirectory)
	}

	/**
	 * Retrieves the HEAD commit hash from the given Git directory.
	 *
	 * @param gitDirectory the base directory of the Git repository.
	 * @return the hash of the HEAD commit.
	 */
	private fun getHeadCommitFromGitDirectory(gitDirectory: Path): String {
		Git.open(gitDirectory.toFile()).use { git ->
			return git.repository.refDatabase.findRef(HEAD_REF).objectId.name
		}
	}

	/**
	 * Traverses the directory tree upwards until it finds a .git directory.
	 * Returns null if no .git directory is found.
	 */
	private fun findGitDirectory(searchDirectory: Path?) =
		generateSequence(searchDirectory) { it.parent }
			.firstOrNull { Files.exists(it.resolve(".git")) }
}
