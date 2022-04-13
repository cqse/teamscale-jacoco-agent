package com.teamscale.tia.maven;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a single commit in a Git repository.
 */
public class GitCommit {

	/** The SHA1 of this commit. */
	public final String sha1;
	/** The timestamp of this commit (Unix epoch in milliseconds). */
	public final long timestamp;
	/** The branch of this commit. */
	public final String branch;

	private GitCommit(String sha1, long timestamp, String branch) {
		this.sha1 = sha1;
		this.timestamp = timestamp;
		this.branch = branch;
	}

	/**
	 * Determines the current HEAD commit in the Git repository located in the or above the given search directory.
	 *
	 * @throws IOException if reading from the Git repository fails or the current directory is not a Git repository.
	 */
	public static GitCommit getGitHeadCommitDescriptor(Path searchDirectory) throws IOException {
		Path gitDirectory = findGitBaseDirectory(searchDirectory);
		Git git = Git.open(gitDirectory.toFile());
		Repository repository = git.getRepository();
		String branch = repository.getBranch();
		RevCommit commit = getCommit(repository, branch);
		long commitTimeSeconds = commit.getCommitTime();
		String ref = repository.getRefDatabase().findRef("HEAD").getObjectId().getName();
		return new GitCommit(ref, commitTimeSeconds * 1000L, branch);
	}

	/**
	 * Traverses the directory tree upwards until it finds a .git directory. Returns null if no .git directory is
	 * found.
	 */
	private static Path findGitBaseDirectory(Path searchDirectory) {
		while (searchDirectory != null) {
			if (Files.exists(searchDirectory.resolve(".git"))) {
				return searchDirectory;
			}
			searchDirectory = searchDirectory.getParent();
		}
		return null;
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
}
