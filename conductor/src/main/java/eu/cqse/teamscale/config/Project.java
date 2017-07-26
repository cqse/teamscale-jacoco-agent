package eu.cqse.teamscale.config;

import eu.cqse.teamscale.test.GitRepositoryHelper;
import eu.cqse.teamscale.test.upload.CommitDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Project {
    public File rootDirectory;
    public File classesDirectory;

    public CommitDescriptor getCommitDescriptor() throws GitAPIException, IOException {
        return GitRepositoryHelper.getHeadCommitDescriptor(rootDirectory);
    }

    public CommitDescriptor getCommitDescriptorFromRef(String ref) throws GitAPIException, IOException {
        return GitRepositoryHelper.getCommitDescriptor(rootDirectory, ref);
    }

}
