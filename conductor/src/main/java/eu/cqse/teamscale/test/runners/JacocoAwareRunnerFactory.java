package eu.cqse.teamscale.test.runners;

import org.junit.runner.Runner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

@SuppressWarnings("unused")
public class JacocoAwareRunnerFactory implements
        ParametersRunnerFactory {
    public Runner createRunnerForTestWithParameters(TestWithParameters test)
            throws InitializationError {
        return new JacocoAwareJUnit4ClassRunnerWithParameters(test);
    }

    private class JacocoAwareJUnit4ClassRunnerWithParameters extends BlockJUnit4ClassRunnerWithParameters {
        JacocoAwareJUnit4ClassRunnerWithParameters(TestWithParameters test) throws InitializationError {
            super(test);
        }

        @Override
        protected Statement methodBlock(final FrameworkMethod frameworkMethod) {
            return new JacocoAwareStatement(super.methodBlock(frameworkMethod), frameworkMethod);
        }
    }

}