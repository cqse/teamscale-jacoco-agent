package com.teamscale.config

import com.teamscale.GitRepositoryHelper
import com.teamscale.client.CommitDescriptor
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

    /** Wraps branch and timestamp in a commit descriptor. */
    fun getCommitDescriptor(): CommitDescriptor {
        return CommitDescriptor(branchName, timestamp)
    }

    /**
     * Checks that a branch name and timestamp are set or can be retrieved from the projects git and
     * stores them for later use.
     * Returns true if validation was successful.
     */
    fun validate(project: Project, testTaskName: String): Boolean {
        return try {
            if (branchName == null || timestamp == null) {
                val commit = GitRepositoryHelper.getHeadCommitDescriptor(project.rootDir)
                branchName = branchName ?: commit.branchName
                timestamp = timestamp ?: commit.timestamp
            }
            true
        } catch (e: IOException) {
            project.logger.error("Could not determine Teamscale upload commit for ${project.name} $testTaskName", e)
            false
        }
    }
}
