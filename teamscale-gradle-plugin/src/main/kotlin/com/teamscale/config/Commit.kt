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

    /**
     * The repository (= identifier of the teamscale connector), the commit belongs to.
     */
    var repositoryId: String? = null
        set(value) {
            field = value?.trim()
        }

    /** Wraps [branchName] and [timestamp] in a commit descriptor. */
    private fun getCommitDescriptor(): CommitDescriptor {
        return CommitDescriptor(branchName, timestamp)
    }

    /**
     * Checks that a [branchName] and [timestamp] are set or can be retrieved from the projects git and
     * stores them for later use.
     */
    fun getOrResolveCommitDescriptor(project: Project): Pair<CommitDescriptor?, String?> {
        try {
            if (branchName == null || timestamp == null || revision == null) {
                val (commit, ref) = GitRepositoryHelper.getHeadCommitDescriptor(project.rootDir)
                branchName = branchName ?: commit.branchName
                timestamp = timestamp ?: commit.timestamp
                this.revision = this.revision ?: ref
            }
            return Pair(getCommitDescriptor(), this.revision)
        } catch (e: IOException) {
            if (branchName != null && timestamp != null) {
                return Pair(getCommitDescriptor(), null)
            }
            if (revision != null) {
                return Pair(null, revision)
            }
            throw GradleException("Could not determine Teamscale upload commit", e)
        }
    }
}
