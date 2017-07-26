package eu.cqse.teamscale.test.listeners;

import eu.cqse.teamscale.test.controllers.JacocoLocalController;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG and JUnit listener that instructs JaCoCo to create one session per test.
 */
public class TestNGListener extends JUnit4Listener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        JacocoLocalController.getInstance().onTestStart(result.getTestClass().getName(), result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        JacocoLocalController.getInstance().onTestFinish(result.getTestClass().getName(), result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        JacocoLocalController.getInstance().onTestFinish(result.getTestClass().getName(), result.getMethod().getMethodName());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        JacocoLocalController.getInstance().onTestFinish(result.getTestClass().getName(), result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        JacocoLocalController.getInstance().onTestFinish(result.getTestClass().getName(), result.getMethod().getMethodName());
    }

    @Override
    public void onStart(ITestContext context) {
        // nop
    }

    @Override
    public void onFinish(ITestContext context) {
        // nop
    }

}
