package eu.cqse.teamscale.jacoco.agent.store.upload.teamscale;

import java.io.Serializable;

/** Holds the branch and timestamp of a commit. */
public class CommitDescriptor implements Serializable {

	/** Branch name of the commit. */
	private final String branch;

	/** Timestamp of the commit. */
	private final String timestamp;

	public CommitDescriptor(String branch, String timestamp) {
		this.branch = branch;
		this.timestamp = timestamp;
	}

	/**
	 * Returns a string representation of the commit in a Teamscale REST API compatible format.
	 */
	@Override
	public String toString() {
		return branch + ":" + timestamp;
	}
}
