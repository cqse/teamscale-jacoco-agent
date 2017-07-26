package eu.cqse.teamscale.test.runners;

import eu.cqse.teamscale.test.controllers.JacocoLocalController;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@SuppressWarnings("WeakerAccess")
public class JacocoAwareStatement extends Statement {

    private final Statement statement;
    private final FrameworkMethod frameworkMethod;

    public JacocoAwareStatement(Statement statement, FrameworkMethod frameworkMethod) {
        this.statement = statement;
        this.frameworkMethod = frameworkMethod;
    }

    @Override
    public void evaluate() throws Throwable {
        try {
            JacocoLocalController.getInstance().onTestStart(frameworkMethod.getDeclaringClass().getCanonicalName(), frameworkMethod.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        statement.evaluate();
        try {
            JacocoLocalController.getInstance().onTestFinish(frameworkMethod.getDeclaringClass().getCanonicalName(), frameworkMethod.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
