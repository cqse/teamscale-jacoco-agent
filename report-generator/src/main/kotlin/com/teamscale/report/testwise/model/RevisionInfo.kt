package com.teamscale.report.testwise.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.teamscale.client.CommitDescriptor
import java.io.Serializable

/** Revision information necessary for uploading reports to Teamscale.  */
class RevisionInfo : Serializable {
	/** The type of revision information.  */
	val type: ERevisionType

	/** The value. Either a commit descriptor or a source control revision, depending on [type].  */
	val value: String?

	@JsonCreator
	constructor(@JsonProperty("type") type: ERevisionType, @JsonProperty("value") value: String) {
		this.type = type
		this.value = value
	}

	/**
	 * Constructor in case you have both fields, and either may be null. If both are set, the commit wins. If both are
	 * null, [type] will be [ERevisionType.REVISION] and [value] will be null.
	 */
	constructor(commit: CommitDescriptor?, revision: String?) {
		if (commit == null) {
			type = ERevisionType.REVISION
			value = revision
		} else {
			type = ERevisionType.COMMIT
			value = commit.toString()
		}
	}
}
