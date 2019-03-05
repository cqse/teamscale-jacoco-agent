package com.teamscale.testimpacted.test_descriptor;

import com.teamscale.client.ClusteredTestDetails;
import org.junit.platform.engine.TestDescriptor;

import java.util.Optional;

public interface ITestDescriptorResolver {

	Optional<String> toUniformPath(TestDescriptor testDescriptor);

	Optional<ClusteredTestDetails> toClusteredTestDetails(TestDescriptor testDescriptor);

	String getEngineId();
}
