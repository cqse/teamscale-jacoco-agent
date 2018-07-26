package eu.cqse.config

import eu.cqse.GitRepositoryHelper
import eu.cqse.teamscale.client.CommitDescriptor
import java.io.File
import java.io.IOException
import java.io.Serializable

class Commit : Serializable {
    /** The branch at which the artifacts belong to.  */
    var branch: String? = null
        set(value) {
            field = value?.trim()
        }

    /** The timestamp of the commit that has been used to generate the artifacts.  */
    var timestamp: String? = null
        set(value) {
            field = value?.trim()
        }

    @Throws(IOException::class)
    fun getCommit(rootDir: File): CommitDescriptor {
        return if (branch == null || timestamp == null) {
            val commit = GitRepositoryHelper.getHeadCommitDescriptor(rootDir)
            CommitDescriptor(branch ?: commit.branchName, timestamp ?: commit.timestamp)
        } else {
            CommitDescriptor(branch, timestamp)
        }
    }

    fun copyWithDefault(toCopy: Commit, default: Commit) {
        branch = toCopy.branch ?: default.branch
        timestamp = toCopy.timestamp ?: default.timestamp
    }
}
