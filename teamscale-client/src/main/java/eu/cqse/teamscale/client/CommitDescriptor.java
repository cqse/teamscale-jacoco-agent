package eu.cqse.teamscale.client;

import java.io.Serializable;
import java.util.Objects;

/** Holds the branch and timestamp of a commit. */
public class CommitDescriptor implements Serializable {

	/** Branch name of the commit. */
	public final String branchName;

	/** Timestamp of the commit. */
	public final long timestamp;

	/**
	 * Selects the commit #previousCommitsShift commits before the given timestamp.
	 * This is useful to select the commit before as baseline by appending p1. The
	 * actual resolution happens in Teamscale.
	 */
	private final int previousCommitsShift;

	/** Constructor. */
	public CommitDescriptor(String branchName, long timestamp) {
		this(branchName, timestamp, 0);
	}

	/** Constructor. */
	private CommitDescriptor(String branchName, long timestamp, int previousCommitsShift) {
		this.branchName = branchName;
		this.timestamp = timestamp;
		this.previousCommitsShift = previousCommitsShift;
	}

	/** Parses the given commit descriptor string. */
	public static CommitDescriptor parse(String commit) {
		if (commit.contains(":")) {
			String[] split = commit.split(":");
			return new CommitDescriptor(split[0], Long.parseLong(split[1]));
		} else {
			return new CommitDescriptor("master", Long.parseLong(commit));
		}
	}

	/** Returns a commit descriptor with appended p1, which teamscale interprets as the commit before. */
	public CommitDescriptor commitBefore() {
		return new CommitDescriptor(branchName, timestamp, 1);
	}

	/** Returns a string representation of the commit in a Teamscale REST API compatible format. */
	@Override
	public String toString() {
		if (previousCommitsShift == 0) {
			return branchName + ":" + timestamp;
		} else {
			return branchName + ":" + timestamp + "p" + previousCommitsShift;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CommitDescriptor that = (CommitDescriptor) o;
		return Objects.equals(branchName, that.branchName) && Objects.equals(timestamp, that.timestamp) && Objects
				.equals(previousCommitsShift, that.previousCommitsShift);
	}

	@Override
	public int hashCode() {
		return Objects.hash(branchName, timestamp, previousCommitsShift);
	}
}
