package com.teamscale.test_impacted.engine;

import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.test_impacted.engine.executor.DummyEngine;
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver;
import org.junit.jupiter.api.Disabled;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

import java.util.Arrays;
import java.util.List;

import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/** Test setup for a mixture of impacted and no impacted tests and two test engines. */
class ImpactedTestEngineWithTwoEnginesTest extends ImpactedTestEngineTestBase {

	private static final String FIRST_TEST_CLASS = "FirstTestClass";
	private static final String OTHER_TEST_CLASS = "OtherTestClass";
	private static final String IGNORED_TEST_CLASS = "IgnoredTestClass";
	private static final String SECOND_TEST_CLASS = "SecondTestClass";
	private static final String IMPACTED_TEST_CASE_1 = "impactedTestCase1()";
	private static final String IMPACTED_TEST_CASE_2 = "impactedTestCase2()";
	private static final String IMPACTED_TEST_CASE_3 = "impactedTestCase3()";
	private static final String IMPACTED_TEST_CASE_4 = "impactedTestCase4()";
	private static final String SKIPPED_IMPACTED_TEST_CASE_ID = "skippedImpactedTestCaseId()";
	private static final String NON_IMPACTED_TEST_CASE_1 = "nonImpactedTestCase1()";
	private static final String NON_IMPACTED_TEST_CASE_2 = "nonImpactedTestCase2()";
	/**
	 * For this test setup we rely on the {@link JUnitJupiterTestDescriptorResolver} for resolving uniform paths and
	 * cluster ids. Therefore, the engine root is set accordingly.
	 */
	private final UniqueId engine1RootId = UniqueId.forEngine("junit-jupiter");
	private final UniqueId engine2RootId = UniqueId.forEngine("exotic-engine");

	/** FirstTestClass contains one impacted and one non-impacted test. */
	private final UniqueId firstTestClassId = engine1RootId.append(
			JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, FIRST_TEST_CLASS);
	private final UniqueId impactedTestCase1Id = firstTestClassId.append(
			JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE,
			IMPACTED_TEST_CASE_1);
	private final UniqueId nonImpactedTestCase1Id = firstTestClassId
			.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, NON_IMPACTED_TEST_CASE_1);

	/**
	 * IgnoredTestClass is ignored (e.g. class is annotated with {@link Disabled}). Hence, it'll be impacted since it
	 * was previously skipped.
	 */
	private final UniqueId ignoredTestClassId = engine1RootId.append(
			JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, IGNORED_TEST_CLASS);
	private final UniqueId impactedTestCase2Id = ignoredTestClassId.append(
			JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE,
			IMPACTED_TEST_CASE_2);
	private final UniqueId nonImpactedTestCase2Id = ignoredTestClassId
			.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, NON_IMPACTED_TEST_CASE_2);

	/**
	 * ImpactedTestClassWithSkippedTest contains two impacted tests of which one is skipped.
	 */
	private final UniqueId secondTestClassId = engine1RootId.append(
			JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, SECOND_TEST_CLASS);
	private final UniqueId impactedTestCase3Id = secondTestClassId.append(
			JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE,
			IMPACTED_TEST_CASE_3);
	private final UniqueId skippedImpactedTestCaseId = secondTestClassId
			.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, SKIPPED_IMPACTED_TEST_CASE_ID);

	/** OtherTestClass contains one impacted and one non-impacted test. */
	private final UniqueId otherTestClassId = engine2RootId.append(
			JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, OTHER_TEST_CLASS);
	private final UniqueId impactedTestCase4Id = otherTestClassId.append(
			JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE,
			IMPACTED_TEST_CASE_4);

	private final TestDescriptor impactedTestCase1 = testCase(impactedTestCase1Id);
	private final TestDescriptor nonImpactedTestCase1 = testCase(nonImpactedTestCase1Id);
	private final TestDescriptor firstTestClass = testContainer(firstTestClassId,
			impactedTestCase1, nonImpactedTestCase1);

	private final TestDescriptor impactedTestCase2 = testCase(impactedTestCase2Id);
	private final TestDescriptor nonImpactedTestCase2 = testCase(nonImpactedTestCase2Id);
	private final TestDescriptor ignoredTestClass = testContainer(ignoredTestClassId,
			impactedTestCase2, nonImpactedTestCase2).skip();

	private final TestExecutionResult failed = failed(new NullPointerException());
	private final TestDescriptor impactedTestCase3 = testCase(impactedTestCase3Id).result(failed);
	private final TestDescriptor skippedImpactedTestCase = testCase(skippedImpactedTestCaseId).skip();
	private final TestDescriptor secondTestClass = testContainer(secondTestClassId,
			impactedTestCase3, skippedImpactedTestCase);

	private final TestDescriptor impactedTestCase4 = testCase(impactedTestCase4Id);
	private final TestDescriptor otherTestClass = testContainer(otherTestClassId, impactedTestCase4);

	private final TestDescriptor testEngine1Root = testContainer(engine1RootId, firstTestClass,
			ignoredTestClass, secondTestClass);

	private final TestDescriptor testEngine2Root = testContainer(engine2RootId, otherTestClass);

	@Override
	public List<TestEngine> getEngines() {
		return Arrays.asList(
				new DummyEngine(testEngine1Root),
				new DummyEngine(testEngine2Root));
	}

	@Override
	public List<PrioritizableTestCluster> getImpactedTests() {
		return Arrays.asList(
				new PrioritizableTestCluster(FIRST_TEST_CLASS,
						singletonList(new PrioritizableTest(FIRST_TEST_CLASS + "/" + IMPACTED_TEST_CASE_1))),
				new PrioritizableTestCluster(OTHER_TEST_CLASS,
						singletonList(new PrioritizableTest(OTHER_TEST_CLASS + "/" + IMPACTED_TEST_CASE_4))),
				new PrioritizableTestCluster(IGNORED_TEST_CLASS,
						singletonList(new PrioritizableTest(IGNORED_TEST_CLASS + "/" + IMPACTED_TEST_CASE_2))),
				new PrioritizableTestCluster(SECOND_TEST_CLASS,
						asList(new PrioritizableTest(SECOND_TEST_CLASS + "/" + IMPACTED_TEST_CASE_3),
								new PrioritizableTest(SECOND_TEST_CLASS + "/" + SKIPPED_IMPACTED_TEST_CASE_ID))));
	}

	@Override
	public void verifyCallbacks(EngineExecutionListener executionListener) {
		// Start of engine 1
		verify(executionListener).executionStarted(testEngine1Root);

		// Execute FirstTestClass.
		verify(executionListener).executionStarted(firstTestClass);
		verify(executionListener).executionStarted(impactedTestCase1);
		verify(executionListener).executionFinished(eq(impactedTestCase1), any());
		verify(executionListener).executionFinished(eq(firstTestClass), any());

		// Execute IgnoredTestClass.
		verify(executionListener).executionStarted(ignoredTestClass);
		verify(executionListener).executionSkipped(eq(impactedTestCase2), any());
		verify(executionListener).executionFinished(ignoredTestClass, successful());

		// Execute SecondTestClass.
		verify(executionListener).executionStarted(secondTestClass);
		verify(executionListener).executionStarted(eq(impactedTestCase3));
		verify(executionListener).executionFinished(impactedTestCase3,
				failed);
		verify(executionListener).executionSkipped(eq(skippedImpactedTestCase), any());
		verify(executionListener).executionFinished(secondTestClass, successful());

		// Finish test engine 1
		verify(executionListener).executionFinished(testEngine1Root, successful());

		// Start of engine 2
		verify(executionListener).executionStarted(testEngine2Root);

		// Execute OtherTestClass.
		verify(executionListener).executionStarted(otherTestClass);
		verify(executionListener).executionStarted(impactedTestCase4);
		verify(executionListener).executionFinished(impactedTestCase4, successful());
		verify(executionListener).executionFinished(otherTestClass, successful());

		// Finish test engine 2
		verify(executionListener).executionFinished(testEngine2Root, successful());
	}
}
