package com.teamscale.report.testwise.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.teamscale.client.CommitDescriptor;

import java.io.Serializable;

/** Revision information necessary for uploading reports to Teamscale. */
public class RevisionInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The type of revision information. */
	private final ERevisionType type;

	/** The value. Either a commit descriptor or a source control revision, depending on {@link #type}. */
	private final String value;

	@JsonCreator
	public RevisionInfo(@JsonProperty("type") ERevisionType type, @JsonProperty("value") String value) {
		this.type = type;
		this.value = value;
	}

	/** Constructor for Commit. */
	public RevisionInfo(CommitDescriptor commit) {
		type = ERevisionType.COMMIT;
		value = commit.toString();
	}

	/** Constructor for Revision. */
	public RevisionInfo(String revision) {
		type = ERevisionType.REVISION;
		value = revision;
	}

	/**
	 * Constructor in case you have both fields, and either may be null. If both are set, the commit wins. If both are
	 * null, {@link #type} will be {@link ERevisionType#REVISION} and {@link #value} will be null.
	 */
	public RevisionInfo(CommitDescriptor commit, String revision) {
		if (commit == null) {
			type = ERevisionType.REVISION;
			value = revision;
		} else {
			type = ERevisionType.COMMIT;
			value = commit.toString();
		}
	}

	public ERevisionType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}
}
