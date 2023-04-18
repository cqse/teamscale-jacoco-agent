package com.teamscale.test_impacted.engine.executor;

import org.junit.platform.engine.TestDescriptor;

/**
 * NOP sorter that does nothing for when the engine is configured to only collect testwise coverage, but not using
 * Teamscale to select or prioritize tests.
 */
public class NOPTestSorter implements ITestSorter {
	@Override
	public void selectAndSort(TestDescriptor rootTestDescriptor) {
		// Nothing to do
	}
}
