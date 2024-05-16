package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.client.CommitDescriptor;

import java.util.Objects;

/** Hold information regarding a commit. */
public class CommitInfo {
	/** The revision information (git hash). */
	public String revision;

	/** The commit descriptor. */
	public CommitDescriptor commit;

	/** Constructor. */
	public CommitInfo(String revision, CommitDescriptor commit) {
		this.revision = revision;
		this.commit = commit;
	}

	@Override
	public String toString() {
		return commit + "/" + revision;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CommitInfo that = (CommitInfo) o;
		return Objects.equals(revision, that.revision) && Objects.equals(commit, that.commit);
	}

	@Override
	public int hashCode() {
		return Objects.hash(revision, commit);
	}
}
