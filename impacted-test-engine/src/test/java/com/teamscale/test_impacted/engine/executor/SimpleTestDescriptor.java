package com.teamscale.test_impacted.engine.executor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.platform.engine.TestExecutionResult.successful;

/** A basic implementation of a {@link TestDescriptor} that can be used during tests. */
public class SimpleTestDescriptor extends AbstractTestDescriptor {

	private final Type type;

	private boolean shouldSkip = false;

	private final List<TestDescriptor> dynamicTests = new ArrayList<>();

	private TestExecutionResult executionResult = successful();

	private SimpleTestDescriptor(UniqueId uniqueId, Type type, String displayName) {
		super(uniqueId, displayName);
		this.type = type;
	}

	@Override
	public Type getType() {
		return type;
	}

	/** Marks the test as being skipped. */
	public SimpleTestDescriptor skip() {
		this.shouldSkip = true;
		return this;
	}

	/** Whether the test should be skipped. */
	public boolean shouldBeSkipped() {
		return shouldSkip;
	}

	/**
	 * The dynamic child tests that should be simulated or just an empty list if the test does not contain dynamic
	 * tests.
	 */
	public List<TestDescriptor> getDynamicTests() {
		return dynamicTests;
	}

	/** Sets the execution result that the engine should report when simulating the test's execution. */
	public SimpleTestDescriptor result(TestExecutionResult executionResult) {
		this.executionResult = executionResult;
		return this;
	}

	public TestExecutionResult getExecutionResult() {
		return this.executionResult;
	}

	/** Creates a {@link TestDescriptor} for a concrete test case without children. */
	public static SimpleTestDescriptor testCase(UniqueId uniqueId) {
		return new SimpleTestDescriptor(uniqueId, Type.TEST, getSimpleDisplayName(uniqueId));
	}

	private static String getSimpleDisplayName(UniqueId uniqueId) {
		return uniqueId.getSegments().get(uniqueId.getSegments().size() - 1).getValue();
	}

	/** Creates a {@link TestDescriptor} for a dynamic test case which registers children during test execution. */
	public static TestDescriptor dynamicTestCase(UniqueId uniqueId, TestDescriptor... dynamicTestCases) {
		SimpleTestDescriptor simpleTestDescriptor = new SimpleTestDescriptor(uniqueId, Type.CONTAINER_AND_TEST,
				getSimpleDisplayName(uniqueId));
		simpleTestDescriptor.dynamicTests.addAll(Arrays.asList(dynamicTestCases));
		return simpleTestDescriptor;
	}

	/**
	 * Creates a {@link TestDescriptor} for a test container (e.g. a test class or test engine) containing other
	 * {@link TestDescriptor} children.
	 */
	public static SimpleTestDescriptor testContainer(UniqueId uniqueId, TestDescriptor... children) {
		SimpleTestDescriptor result = new SimpleTestDescriptor(uniqueId, Type.CONTAINER,
				getSimpleDisplayName(uniqueId));
		for (TestDescriptor child : children) {
			result.addChild(child);
		}
		return result;
	}
}
