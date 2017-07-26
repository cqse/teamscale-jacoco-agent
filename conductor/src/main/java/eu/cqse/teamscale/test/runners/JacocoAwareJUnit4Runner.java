package eu.cqse.teamscale.test.runners;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * As a workaround until the pull request gets merged
 * https://github.com/gradle/gradle/pull/1725 JUnit listener in gradle
 */
@SuppressWarnings("unused")
public class JacocoAwareJUnit4Runner extends BlockJUnit4ClassRunner {

    public JacocoAwareJUnit4Runner(Class<?> aClass) throws InitializationError {
        super(aClass);
    }

    @Override
    protected Statement methodBlock(final FrameworkMethod frameworkMethod) {
        return new JacocoAwareStatement(super.methodBlock(frameworkMethod), frameworkMethod);
    }
}