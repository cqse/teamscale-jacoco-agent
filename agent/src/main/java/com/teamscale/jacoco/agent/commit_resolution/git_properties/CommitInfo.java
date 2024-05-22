package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.StringUtils;

import java.util.Objects;

/** Hold information regarding a commit. */
public class CommitInfo {
	/** The revision information (git hash). */
	public String revision;

	/** The commit descriptor. */
	public CommitDescriptor commit;

	/**
	 * If the commit property is set via the <code>teamscale.timestamp</code> property in a git.properties file, this
	 * should be preferred to the revision. For details see <a
	 * href="https://cqse.atlassian.net/browse/TS-38561">TS-38561</a>.
	 */
	public boolean preferCommitDescriptorOverRevision = false;

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

	/**
	 * Returns true if one of or both, revision and commit, are set
	 */
	public boolean isEmpty() {
		return StringUtils.isEmpty(revision) && commit == null;
	}
}
