package com.teamscale.client

import java.io.Serializable

/** Holds the branch and timestamp of a commit.  */
data class CommitDescriptor(
	/** Branch name of the commit.  */
	@JvmField val branchName: String,
	/**
	 * Timestamp of the commit. The timestamp is a string here because we also want to be able to handle HEAD and
	 * 123456p1.
	 */
	@JvmField val timestamp: String
) : Serializable {
	/** Constructor.  */
	constructor(branchName: String, timestamp: Long) : this(branchName, timestamp.toString())

	/** Returns a string representation of the commit in a Teamscale REST API compatible format.  */
	override fun toString() = "$branchName:$timestamp"

	companion object {
		/** Parses the given commit descriptor string.  */
		@JvmStatic
		fun parse(commit: String?): CommitDescriptor? {
			if (commit == null) {
				return null
			}
			if (commit.contains(":")) {
				val split = commit.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				return CommitDescriptor(split[0], split[1])
			} else {
				return CommitDescriptor("master", commit)
			}
		}
	}
}
