package com.teamscale.config.internal

import com.teamscale.client.CommitDescriptor

/** Abstraction over commit or revision where exactly one of both fields returns null and the other one non-null. */
sealed interface CommitInfo {
	/** The commit descriptor */
	val commit: CommitDescriptor?
	/** The commit revision */
	val revision: String?
}

/** Abstraction over timestamp or revision where exactly one of both fields returns null and the other one non-null. */
sealed interface BaselineInfo {
	/** The commit timestamp (in milliseconds since 1970) or one of the allowed encodings in Teamscale, e.g., 1234p1. */
	val timestamp: String?
	/** The commit revision */
	val revision: String?
}

/** Wrapper around CommitDescriptor. */
class BranchAndTimestamp(commitDescriptor: CommitDescriptor): CommitInfo {
	override val revision = null
	override val commit = commitDescriptor
	override fun toString() = "commit $commit"
}

/** Wrapper around revision. */
class Revision(override val revision: String): CommitInfo, BaselineInfo {
	override val commit = null
	override val timestamp = null
	override fun toString() = "revision $revision"
}

/** Wrapper around timestamp. */
class Timestamp(override val timestamp: String): BaselineInfo {
	override val revision = null
	override fun toString() = "timestamp $revision"
}

