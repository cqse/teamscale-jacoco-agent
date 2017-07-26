package eu.cqse.teamscale.test.runners;

import eu.cqse.teamscale.test.controllers.JacocoLocalController;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.Statement;

/**
 * As a workaround until the pull request gets merged
 * https://github.com/gradle/gradle/pull/1725 JUnit listener in gradle
 */
@SuppressWarnings("unused")
public class JacocoAwareJUnit4ParameterizedRunner extends Parameterized {

    public JacocoAwareJUnit4ParameterizedRunner(Class<?> aClass) throws Throwable {
        super(aClass);
    }

    @Override
    protected Statement childrenInvoker(final RunNotifier notifier) {
        final Statement statement = JacocoAwareJUnit4ParameterizedRunner.super.childrenInvoker(notifier);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                JacocoLocalController.getInstance().doWriteImmediately = false;
                statement.evaluate();
                JacocoLocalController.getInstance().writeNow();
                JacocoLocalController.getInstance().doWriteImmediately = true;
            }
        };
    }
}