package com.teamscale.testimpacted.test_descriptor;

import com.teamscale.client.ClusteredTestDetails;
import org.junit.platform.engine.TestDescriptor;

import java.util.Optional;

public abstract class JUnitTestDescriptorResolverBase implements ITestDescriptorResolver {

	@Override
	public Optional<String> toUniformPath(TestDescriptor testDescriptor) {
		return getClassSegment(testDescriptor)
				.map(className -> className.replace(".", "/") + "/" + testDescriptor.getLegacyReportingName());
	}

	@Override
	public Optional<ClusteredTestDetails> toClusteredTestDetails(TestDescriptor testDescriptor) {
		return toUniformPath(testDescriptor).map(uniformPath -> {
			String sourcePath = TestDescriptorUtils.getSource(testDescriptor);
			String clusterId = getClassSegment(testDescriptor).orElse(uniformPath);
			return new ClusteredTestDetails(uniformPath, sourcePath, null, clusterId);
		});
	}

	protected abstract Optional<String> getClassSegment(TestDescriptor testDescriptor);

}
