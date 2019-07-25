package com.teamscale.test_impacted.engine.executor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/** A basic implementation of a {@link TestDescriptor} that can be used during tests. */
public class SimpleTestDescriptor extends AbstractTestDescriptor {

	private final Type type;

	private SimpleTestDescriptor(UniqueId uniqueId, Type type, String displayName) {
		super(uniqueId, displayName);
		this.type = type;
	}

	@Override
	public Type getType() {
		return type;
	}

	/** Creates a {@link TestDescriptor} for a concrete test case without children. */
	public static TestDescriptor testCase(UniqueId uniqueId) {
		return new SimpleTestDescriptor(uniqueId, Type.TEST, getSimpleDisplayName(uniqueId));
	}

	private static String getSimpleDisplayName(UniqueId uniqueId) {
		return uniqueId.getSegments().get(uniqueId.getSegments().size() - 1).getValue();
	}

	/** Creates a {@link TestDescriptor} for a dynamic test case which registers children during test execution. */
	public static TestDescriptor dynamicTestCase(UniqueId uniqueId) {
		return new SimpleTestDescriptor(uniqueId, Type.CONTAINER_AND_TEST, getSimpleDisplayName(uniqueId));
	}

	/**
	 * Creates a {@link TestDescriptor} for a test container (e.g. a test class or test engine) containing other {@link
	 * TestDescriptor} children.
	 */
	public static TestDescriptor testContainer(UniqueId uniqueId, TestDescriptor... children) {
		TestDescriptor result = new SimpleTestDescriptor(uniqueId, Type.CONTAINER, getSimpleDisplayName(uniqueId));
		for (TestDescriptor child : children) {
			result.addChild(child);
		}
		return result;
	}
}
