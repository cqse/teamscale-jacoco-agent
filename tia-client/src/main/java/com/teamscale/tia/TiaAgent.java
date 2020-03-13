package com.teamscale.tia;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTestCluster;
import okhttp3.HttpUrl;

import java.util.List;

public class TiaAgent {

	private final boolean includeNonImpactedTests;
	private final ITestwiseCoverageAgentApi api;

	public TiaAgent(boolean includeNonImpactedTests, HttpUrl url) {
		this.includeNonImpactedTests = includeNonImpactedTests;
		api = ITestwiseCoverageAgentApi.createService(url);
	}

	public TestRun startTestRun(List<ClusteredTestDetails> availableTests,
								Long baseline) throws AgentHttpRequestFailedException {
		List<PrioritizableTestCluster> clusters = AgentCommunicationUtils.handleRequestError(
				api.testRunStarted(includeNonImpactedTests, baseline, availableTests), "Failed to start the test run");
		return new TestRun(api, clusters);
	}


}
