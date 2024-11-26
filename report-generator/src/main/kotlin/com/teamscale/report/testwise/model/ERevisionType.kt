package com.teamscale.report.testwise.model

/** Type of revision information.  */
enum class ERevisionType {
	/** Commit descriptor in the format branch:timestamp.  */
	COMMIT,

	/** Source control revision, e.g. SVN revision or Git hash.  */
	REVISION
}
