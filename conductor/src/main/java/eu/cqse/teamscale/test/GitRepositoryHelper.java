package eu.cqse.teamscale.test;

import eu.cqse.teamscale.test.upload.CommitDescriptor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class GitRepositoryHelper {
    private GitRepositoryHelper() {
    }

    public static CommitDescriptor getHeadCommitDescriptor(File baseDirectory) throws GitAPIException, IOException {
        Git git = Git.open(baseDirectory);
        Iterable<RevCommit> res = git.log().setMaxCount(1).call();
        long time = res.iterator().next().getCommitTime();
        return new CommitDescriptor(git.getRepository().getBranch(), time * 1000L);
    }

    public static CommitDescriptor getCommitDescriptor(File rootDirectory, String ref) throws IOException, GitAPIException {
        Git git = Git.open(rootDirectory);

        Ref rev = git.getRepository().getRef(ref);
        if (rev == null) {
            throw new RuntimeException("No commit found for reference " + ref);
        }
        Iterator<RevCommit> commitLog = git.log().add(rev.getObjectId()).call().iterator();
        RevCommit commit = commitLog.next();

        for (ReflogEntry entry : git.reflog().call()) {
            if (!entry.getOldId().getName().equals(commit.getName())) {
                continue;
            }

            CheckoutEntry checkOutEntry = entry.parseCheckout();
            if (checkOutEntry != null) {
                String branch = checkOutEntry.getFromBranch();
                return new CommitDescriptor(branch, commit.getCommitTime() * 1000L);
            }
        }
        return new CommitDescriptor(Constants.MASTER, commit.getCommitTime() * 1000L);
    }
}
