package eu.cqse.teamscale.test.listeners;

import eu.cqse.teamscale.test.controllers.JacocoLocalController;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * JUnit listener that instructs JaCoCo to create one session per test.
 */
public class JUnit4Listener extends RunListener {

    @Override
    public void testStarted(Description description) {
        JacocoLocalController.getInstance().onTestStart(description.getClassName(), description.getMethodName());
    }

    @Override
    public void testFinished(Description description) {
        JacocoLocalController.getInstance().onTestFinish(description.getClassName(), description.getMethodName());
    }
}