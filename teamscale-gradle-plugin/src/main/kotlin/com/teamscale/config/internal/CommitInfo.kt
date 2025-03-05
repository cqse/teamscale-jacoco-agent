package com.teamscale.config.internal

import com.teamscale.client.CommitDescriptor

sealed interface CommitInfo {
	val commit: CommitDescriptor?
	val revision: String?
}

class BranchAndTimestamp(commitDescriptor: CommitDescriptor): CommitInfo {
	override val revision = null
	override val commit = commitDescriptor
	override fun toString() = "commit $commit"
}

class Revision(override val revision: String): CommitInfo {
	override val commit = null
	override fun toString() = "revision $revision"
}

