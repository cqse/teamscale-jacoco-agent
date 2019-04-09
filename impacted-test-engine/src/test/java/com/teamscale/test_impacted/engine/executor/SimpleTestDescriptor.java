package com.teamscale.test_impacted.engine.executor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/** A basic implementation of a {@link TestDescriptor} that can be used during tests. */
public class SimpleTestDescriptor extends AbstractTestDescriptor {

	private final Type type;

	private SimpleTestDescriptor(UniqueId uniqueId, Type type) {
		super(uniqueId, uniqueId.toString());
		this.type = type;
	}

	@Override
	public Type getType() {
		return type;
	}

	/** Creates a {@link TestDescriptor} for a concrete test case without children. */
	public static TestDescriptor testCase(UniqueId uniqueId) {
		return new SimpleTestDescriptor(uniqueId, Type.TEST);
	}

	/** Creates a {@link TestDescriptor} for a dynamic test case which registers children during test execution. */
	public static TestDescriptor dynamicTestCase(UniqueId uniqueId) {
		return new SimpleTestDescriptor(uniqueId, Type.CONTAINER_AND_TEST);
	}

	/**
	 * Creates a {@link TestDescriptor} for a test container (e.g. a test class or test engine) containing other {@link
	 * TestDescriptor} children.
	 */
	public static TestDescriptor testContainer(UniqueId uniqueId, TestDescriptor... children) {
		TestDescriptor result = new SimpleTestDescriptor(uniqueId, Type.CONTAINER);
		for (TestDescriptor child : children) {
			result.addChild(child);
		}
		return result;
	}
}
