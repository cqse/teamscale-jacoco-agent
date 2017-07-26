package eu.cqse.teamscale.test.upload;

public class CommitDescriptor {
    public final String branch;
    public final long timestamp;

    public CommitDescriptor(String branch, long timestamp) {
        this.branch = branch;
        this.timestamp = timestamp;
    }

    public static CommitDescriptor parse(String commit) {
        if(commit.contains(":")) {
            String[] split = commit.split(":");
            return new CommitDescriptor(split[0], Long.parseLong(split[1]));
        } else {
            return new CommitDescriptor("master", Long.parseLong(commit));
        }
    }

    @Override
    public String toString() {
        return branch + ":" + timestamp;
    }
}
