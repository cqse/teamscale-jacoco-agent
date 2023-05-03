package com.teamscale.test_impacted.engine;

import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.test_impacted.engine.executor.DummyEngine;
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.mockito.Mockito;

import java.util.List;

import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.dynamicTestCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static java.util.Collections.singletonList;
import static org.junit.platform.engine.TestExecutionResult.successful;

/** Test setup for JUnit Jupiter dynamic tests. */
class ImpactedTestEngineWithDynamicTestsTest extends ImpactedTestEngineTestBase {
	/**
	 * For this test setup we rely on the {@link JUnitJupiterTestDescriptorResolver} for resolving uniform paths and
	 * cluster ids. Therefore, the engine root is set accordingly.
	 */
	private final UniqueId engineRootId = UniqueId.forEngine("junit-jupiter");

	private final UniqueId dynamicTestClassId = engineRootId.append(JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, "example.DynamicTest");
	private final UniqueId dynamicTestId = dynamicTestClassId.append(JUnitJupiterTestDescriptorResolver.TEST_FACTORY_SEGMENT_TYPE, "testFactory()");
	private final UniqueId dynamicallyRegisteredTestId = dynamicTestId.append(JUnitJupiterTestDescriptorResolver.DYNAMIC_TEST_SEGMENT_TYPE, "#1");

	private final TestDescriptor dynamicallyRegisteredTestCase = testCase(dynamicallyRegisteredTestId);
	private final TestDescriptor dynamicTestCase = dynamicTestCase(dynamicTestId,
			dynamicallyRegisteredTestCase);
	private final TestDescriptor dynamicTestClassCase = testContainer(dynamicTestClassId,
			dynamicTestCase);
	private final TestDescriptor testRoot = testContainer(engineRootId, dynamicTestClassCase);

	@Override
	public List<TestEngine> getEngines() {
		return singletonList(new DummyEngine(testRoot));
	}

	@Override
	public List<PrioritizableTestCluster> getImpactedTests() {
		return singletonList(
				new PrioritizableTestCluster("example/DynamicTest",
						singletonList(new PrioritizableTest("example/DynamicTest/testFactory()"))));
	}

	@Override
	public void verifyCallbacks(EngineExecutionListener executionListener) {
		// First the parents test descriptors are started in order.
		Mockito.verify(executionListener).executionStarted(testRoot);
		Mockito.verify(executionListener).executionStarted(dynamicTestClassCase);
		Mockito.verify(executionListener).executionStarted(dynamicTestCase);

		// Test case is added dynamically and executed.
		dynamicTestCase.addChild(dynamicallyRegisteredTestCase);
		Mockito.verify(executionListener).dynamicTestRegistered(dynamicallyRegisteredTestCase);
		Mockito.verify(executionListener).executionStarted(dynamicallyRegisteredTestCase);
		Mockito.verify(executionListener).executionFinished(dynamicallyRegisteredTestCase, successful());

		// Parent test descriptors are also finished.
		Mockito.verify(executionListener).executionFinished(dynamicTestCase, successful());
		Mockito.verify(executionListener).executionFinished(dynamicTestClassCase, successful());
		Mockito.verify(executionListener).executionFinished(testRoot, successful());
	}
}
