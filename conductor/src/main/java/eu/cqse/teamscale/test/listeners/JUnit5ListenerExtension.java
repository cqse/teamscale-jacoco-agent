package eu.cqse.teamscale.test.listeners;

import eu.cqse.teamscale.test.controllers.JacocoLocalController;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.TestExtensionContext;

import java.lang.reflect.Method;
import java.util.Optional;

@SuppressWarnings("Since15")
public class JUnit5ListenerExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(TestExtensionContext context) throws Exception {
        JacocoLocalController.getInstance().onTestStart(getTestClass(context), getTestName(context));
    }

    @Override
    public void afterEach(TestExtensionContext context) throws Exception {
        JacocoLocalController.getInstance().onTestFinish(getTestClass(context), getTestName(context));
    }

    private String getTestClass(TestExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isPresent()) {
            return testClass.get().getCanonicalName();
        }
        return "";
    }

    private String getTestName(TestExtensionContext context) {
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isPresent()) {
            return testMethod.get().getName();
        }
        return "";
    }
}
