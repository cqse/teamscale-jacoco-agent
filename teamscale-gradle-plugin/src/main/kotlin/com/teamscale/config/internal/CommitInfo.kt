package com.teamscale.config.internal

import com.teamscale.client.CommitDescriptor

sealed interface CommitInfo {
	val commit: CommitDescriptor?
	val revision: String?
}

sealed interface BaselineInfo {
	val timestamp: String?
	val revision: String?
}

class BranchAndTimestamp(commitDescriptor: CommitDescriptor): CommitInfo {
	override val revision = null
	override val commit = commitDescriptor
	override fun toString() = "commit $commit"
}

class Revision(override val revision: String): CommitInfo, BaselineInfo {
	override val commit = null
	override val timestamp = null
	override fun toString() = "revision $revision"
}

class Timestamp(override val timestamp: String): BaselineInfo {
	override val revision = null
	override fun toString() = "timestamp $revision"
}

