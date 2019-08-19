package com.teamscale.test_impacted.engine.executor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * {@link TestDescriptor} which delegates to another {@link TestDescriptor}. Must be used and updated to reflect the
 * original {@link TestDescriptor} delegated to if children are added or removed.
 */
public class DelegatingTestDescriptor extends AbstractTestDescriptor {

	private final TestDescriptor delegateTestDescriptor;

	DelegatingTestDescriptor(TestDescriptor delegateTestDescriptor) {
		super(delegateTestDescriptor.getUniqueId(), delegateTestDescriptor.getDisplayName());
		this.delegateTestDescriptor = delegateTestDescriptor;
	}

	@Override
	public Type getType() {
		return delegateTestDescriptor.getType();
	}
}
