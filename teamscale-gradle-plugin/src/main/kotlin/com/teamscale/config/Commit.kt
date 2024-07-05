package com.teamscale.config

import com.teamscale.GitRepositoryHelper
import com.teamscale.client.CommitDescriptor
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.IOException
import java.io.Serializable

/** The commit object which holds the end commit for which we do Test Impact Analysis. */
class Commit : Serializable {

    /** The branch to which the artifacts belong to. */
    var branchName: String? = null
        set(value) {
            field = value?.trim()
        }

    /** The timestamp of the commit that has been used to generate the artifacts. */
    var timestamp: String? = null
        set(value) {
            field = value?.trim()
        }

    /**
     * The revision of the commit that the artifacts should be uploaded to.
     * This is e.g. the SHA1 hash of the commit in Git or the revision of the commit in SVN.
     */
    var revision: String? = null
        set(value) {
            field = value?.trim()
        }

    private var resolvedRevision: String? = null
    private var resolvedCommit: CommitDescriptor? = null

    /**
     * Checks that a branch name and timestamp are set or can be retrieved from the projects git and
     * stores them for later use.
     */
    fun getOrResolveCommitDescriptor(project: Project): Pair<CommitDescriptor?, String?> {
        try {
            // If timestamp and branch are set manually, prefer to use them
            if (branchName != null && timestamp != null) {
                return Pair(CommitDescriptor(branchName, timestamp), null)
            }
            // If revision is set manually, use as 2nd option
            if (revision != null) {
                return Pair(null, revision)
            }
            // Otherwise fall back to getting the information from the git repository
            if (resolvedRevision == null && resolvedCommit == null) {
                val (commit, ref) = GitRepositoryHelper.getHeadCommitDescriptor(project.rootDir)
                resolvedRevision = ref
                resolvedCommit = commit
            }
            return Pair(resolvedCommit, resolvedRevision)
        } catch (e: IOException) {
            throw GradleException("Could not determine Teamscale upload commit", e)
        }
    }
}
