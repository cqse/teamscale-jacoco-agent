package com.teamscale.tia.maven;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;

/**
 * Represents a single commit in a Git repository.
 */
public class GitCommit {

	public final String ref;
	public final long timestamp;
	public final String branch;

	private GitCommit(String ref, long timestamp, String branch) {
		this.ref = ref;
		this.timestamp = timestamp;
		this.branch = branch;
	}

	/**
	 * Determines the current HEAD commit in the Git repository located in the given base directory.
	 *
	 * @throws IOException if reading from the Git repository fails or the current directory is not a Git repository.
	 */
	public static GitCommit getGitHeadCommitDescriptor(File baseDirectory) throws IOException {
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
}
