package com.teamscale.client

import java.io.Serializable
import java.util.Objects

/** Holds the branch and timestamp of a commit.  */
class CommitDescriptor
/** Constructor.  */
    (
    /** Branch name of the commit.  */
    val branchName: String,
    /**
     * Timestamp of the commit.
     * The timestamp is a string here because be also want to be able to handle HEAD and 123456p1.
     */
    val timestamp: String
) : Serializable {

    /** Constructor.  */
    constructor(branchName: String, timestamp: Long) : this(branchName, timestamp.toString()) {}

    /** Returns a string representation of the commit in a Teamscale REST API compatible format.  */
    override fun toString(): String {
        return "$branchName:$timestamp"
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as CommitDescriptor?
        return branchName == that!!.branchName && timestamp == that.timestamp
    }

    override fun hashCode(): Int {
        return Objects.hash(branchName, timestamp)
    }

    companion object {

        /** Parses the given commit descriptor string.  */
        fun parse(commit: String): CommitDescriptor {
            if (commit.contains(":")) {
                val split = commit.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                return CommitDescriptor(split[0], split[1])
            } else {
                return CommitDescriptor("master", commit)
            }
        }
    }
}
