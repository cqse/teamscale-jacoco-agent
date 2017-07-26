package eu.cqse.teamscale.test.runners;

import eu.cqse.teamscale.config.CoverageConfiguration;
import eu.cqse.teamscale.test.TestCase;
import eu.cqse.teamscale.test.upload.CommitDescriptor;
import eu.cqse.teamscale.test.upload.TeamscaleClient;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.IOException;
import java.util.List;

public class SuggestedTestsRunner {
    @SuppressWarnings("ThrowableNotThrown")
    public static void main(String[] args) throws GitAPIException, IOException {
        CoverageConfiguration config = CoverageConfiguration.load(args[0]);
        TeamscaleClient client = new TeamscaleClient(config.teamscale);
        String baselineCommitRef = args[1];
        CommitDescriptor baseline = config.project.getCommitDescriptorFromRef(baselineCommitRef);
        CommitDescriptor head = new CommitDescriptor("master", 1492856616000L);//config.project.getCommitDescriptor();
        List<TestCase> tests = client.getSuggestedTests(baseline, head).body();

        System.out.println("Baseline: " + baseline);
        System.out.println("Head: " + head);
        int succeeded = 0, failed = 0, dontExist = 0;
        for (TestCase test : tests) {
            try {
                Class<?> clazz = Class.forName(test.setName);
                Result run = new JUnitCore().run(Request.method(clazz, test.testName));
                if (run.wasSuccessful()) {
                    succeeded++;
                } else {
                    failed++;
                }
                for (Failure failure : run.getFailures()) {
                    if (failure.getException().getMessage().startsWith("No tests found matching Method ")) {
                        System.err.println("Test " + test.setName + "." + test.testName + " not found!");
                        failed--;
                        dontExist++;
                        break;
                    }
                    System.err.println(failure.getDescription());
                    failure.getException().printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                dontExist++;
                System.err.println("Test " + test.setName + "." + test.testName + " not found!");
            }
        }
        System.out.println(tests.size() + " tests total");
        System.out.println(succeeded + " tests successful");
        System.out.println(failed + " tests failed");
        System.out.println(dontExist + " tests don't exist");
    }
}
