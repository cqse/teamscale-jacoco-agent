package com.teamscale.test_impacted.engine;

import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.test_impacted.engine.executor.DummyEngine;
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import java.util.Collections;
import java.util.List;

import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static java.util.Collections.singletonList;

/** Test setup where no test is impacted. */
class NoImpactedTestsTest extends ImpactedTestEngineTestBase {

	private static final String FIRST_TEST_CLASS = "FirstTestClass";
	private static final String NON_IMPACTED_TEST_CASE_1 = "nonImpactedTestCase1()";
	/**
	 * For this test setup we rely on the {@link JUnitJupiterTestDescriptorResolver} for resolving uniform paths and
	 * cluster ids. Therefore, the engine root is set accordingly.
	 */
	private final UniqueId engine1RootId = UniqueId.forEngine("junit-jupiter");

	/** FirstTestClass contains one non-impacted test. */
	private final UniqueId firstTestClassId = engine1RootId.append(
			JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, FIRST_TEST_CLASS);
	private final UniqueId nonImpactedTestCase1Id = firstTestClassId
			.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, NON_IMPACTED_TEST_CASE_1);
	private final TestDescriptor nonImpactedTestCase1 = testCase(nonImpactedTestCase1Id);
	private final TestDescriptor firstTestClass = testContainer(firstTestClassId, nonImpactedTestCase1);

	private final TestDescriptor testEngine1Root = testContainer(engine1RootId, firstTestClass);

	@Override
	public List<TestEngine> getEngines() {
		return singletonList(new DummyEngine(testEngine1Root));
	}

	@Override
	public List<PrioritizableTestCluster> getImpactedTests() {
		return Collections.emptyList();
	}

	@Override
	public void verifyCallbacks(EngineExecutionListener executionListener) {

	}
}
