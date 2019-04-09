package com.teamscale.test_impacted.engine.executor;


import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;

import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.dynamicTestCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static org.junit.platform.engine.TestExecutionResult.successful;

/** Tests for {@link AutoSkippingEngineExecutionListener}. */
class AutoSkippingEngineExecutionListenerTest {

	private AutoSkippingEngineExecutionListener executionListener;

	private final EngineExecutionListener executionListenerMock = Mockito.mock(EngineExecutionListener.class);

	private final UniqueId rootId = UniqueId.forEngine("dummy");

	private AutoSkippingEngineExecutionListener getSkippingExecutionListener(TestDescriptor testRoot,
																			 UniqueId... dynamicTestId) {
		return new AutoSkippingEngineExecutionListener(
				new HashSet<>(Arrays.asList(dynamicTestId)), executionListenerMock, testRoot);
	}

	@Test
	void testDynamicTestRegistration() {
		UniqueId dynamicTestId = rootId.append("PARAMETERIZED_TEST", "parameterizedTest()");
		UniqueId dynamicallyRegisteredTestId = dynamicTestId.append("TEST_CASE", "testCase#1");

		TestDescriptor dynamicTestCase = dynamicTestCase(dynamicTestId);
		TestDescriptor testRoot = testContainer(rootId, dynamicTestCase);
		TestDescriptor dynamicallyRegisteredTestCase = testCase(dynamicallyRegisteredTestId);

		executionListener = getSkippingExecutionListener(testRoot, dynamicTestId);

		// First the parents test descriptors are started in order.
		executionListener.executionStarted(testRoot);
		Mockito.verify(executionListenerMock).executionStarted(testRoot);
		executionListener.executionStarted(dynamicTestCase);
		Mockito.verify(executionListenerMock).executionStarted(dynamicTestCase);

		// Test case is added dynamically and executed.
		dynamicTestCase.addChild(dynamicallyRegisteredTestCase);
		executionListener.dynamicTestRegistered(dynamicallyRegisteredTestCase);
		Mockito.verify(executionListenerMock).dynamicTestRegistered(dynamicallyRegisteredTestCase);
		executionListener.executionStarted(dynamicallyRegisteredTestCase);
		Mockito.verify(executionListenerMock).executionStarted(dynamicallyRegisteredTestCase);
		executionListener.executionFinished(dynamicallyRegisteredTestCase, successful());
		Mockito.verify(executionListenerMock).executionFinished(dynamicallyRegisteredTestCase, successful());

		// Parent test descriptors are also finished.
		executionListener.executionFinished(dynamicTestCase, successful());
		Mockito.verify(executionListenerMock).executionFinished(dynamicTestCase, successful());
		executionListener.executionFinished(testRoot, successful());
		Mockito.verify(executionListenerMock).executionFinished(testRoot, successful());
	}

	@Test
	void testAutoSkipping() {
		UniqueId testClassId = rootId.append("CLASS", "TestClass");
		UniqueId impactedTestId = testClassId.append("TEST", "impactedTest()");
		UniqueId nonImpactedTestId = testClassId.append("TEST", "impactedTest()");

		TestDescriptor impactedTest = testCase(impactedTestId);
		TestDescriptor nonImpactedTest = testCase(nonImpactedTestId);
		TestDescriptor testClass = testContainer(testClassId, impactedTest, nonImpactedTest);
		TestDescriptor testRoot = testContainer(rootId, testClass);

		executionListener = getSkippingExecutionListener(testRoot, impactedTestId);

		// First the parents test descriptors are started in order.
		executionListener.executionStarted(testRoot);
		Mockito.verify(executionListenerMock).executionStarted(testRoot);
		executionListener.executionStarted(testClass);
		Mockito.verify(executionListenerMock).executionStarted(testClass);

		executionListener.executionStarted(impactedTest);
		Mockito.verify(executionListenerMock).executionStarted(impactedTest);
		executionListener.executionFinished(impactedTest, successful());
		Mockito.verify(executionListenerMock).executionFinished(impactedTest, successful());
		executionListener.executionFinished(testClass, successful());
		Mockito.verify(executionListenerMock).executionFinished(nonImpactedTest, successful());
		Mockito.verify(executionListenerMock).executionFinished(testClass, successful());
		executionListener.executionFinished(testRoot, successful());
		Mockito.verify(executionListenerMock).executionFinished(testRoot, successful());
	}
}
