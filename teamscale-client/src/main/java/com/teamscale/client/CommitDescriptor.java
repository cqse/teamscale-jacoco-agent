package com.teamscale.client;

import java.io.Serializable;
import java.util.Objects;

/** Holds the branch and timestamp of a commit. */
public class CommitDescriptor implements Serializable {

	/** Branch name of the commit. */
	public final String branchName;

	/**
	 * Timestamp of the commit. The timestamp is a string here because be also want to be able to handle HEAD and
	 * 123456p1.
	 */
	public final String timestamp;

	/** Constructor. */
	public CommitDescriptor(String branchName, String timestamp) {
		this.branchName = branchName;
		this.timestamp = timestamp;
	}

	/** Constructor. */
	public CommitDescriptor(String branchName, long timestamp) {
		this(branchName, String.valueOf(timestamp));
	}

	/** Parses the given commit descriptor string. */
	public static CommitDescriptor parse(String commit) {
		if (commit == null) {
			return null;
		}
		if (commit.contains(":")) {
			String[] split = commit.split(":");
			return new CommitDescriptor(split[0], split[1]);
		} else {
			return new CommitDescriptor("master", commit);
		}
	}

	/** Returns a string representation of the commit in a Teamscale REST API compatible format. */
	@Override
	public String toString() {
		return branchName + ":" + timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CommitDescriptor that = (CommitDescriptor) o;
		return Objects.equals(branchName, that.branchName) &&
				Objects.equals(timestamp, that.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(branchName, timestamp);
	}
}
