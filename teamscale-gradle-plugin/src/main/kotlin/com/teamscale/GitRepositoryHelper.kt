package com.teamscale

import com.teamscale.client.CommitDescriptor
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import java.io.IOException

/** Helper class for Git repository actions. */
object GitRepositoryHelper {

    /** Returns the last commit in the given git repository. */
    @Throws(IOException::class)
    fun getHeadCommitDescriptor(baseDirectory: File): CommitDescriptor {
        val git = Git.open(baseDirectory)
        val repository = git.repository
        val branch = repository.branch
        val commit = getCommit(repository, branch)
        val time = commit.commitTime.toLong()
        return CommitDescriptor(branch, time * 1000L)
    }

    /** Returns the commit denoted by the given commit id/tag/head.  */
    @Throws(IOException::class)
    private fun getCommit(repository: Repository, revisionBranchOrTag: String): RevCommit {
        val revWalk = RevWalk(repository)
        try {
            val head = repository.getRef(revisionBranchOrTag)
            return if (head != null) {
                revWalk.parseCommit(head.leaf.objectId)
            } else revWalk.parseCommit(ObjectId.fromString(revisionBranchOrTag))

        } finally {
            revWalk.release()
        }
    }
}
