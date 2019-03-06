package com.teamscale.testimpacted.junit.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.testimpacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.testimpacted.junit.ImpactedTestEngine;
import com.teamscale.testimpacted.junit.executor.DelegatingTestExecutor;
import com.teamscale.testimpacted.junit.executor.ITestExecutor;
import com.teamscale.testimpacted.junit.executor.ImpactedTestsExecutor;
import com.teamscale.testimpacted.junit.executor.TestWiseCoverageCollectingTestExecutor;
import okhttp3.HttpUrl;
import org.junit.platform.commons.util.Preconditions;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Represents options for the {@link ImpactedTestEngine}. */
public class TestEngineOptions {

	/** The server options. May not be null. */
	private ServerOptions serverOptions;

	/** The partition to upload test details to and get impacted tests from. If null all partitions are used. */
	private String partition;

	/** Executes all tests, not only impacted ones if set. Defaults to false. */
	private boolean runAllTests = false;

	/** Executes all tests, not only impacted ones if set. Defaults to true. */
	private boolean runImpacted = true;

	/**
	 * The baseline. Only code changes after the baseline are considered for determining impacted tests. May be null to
	 * indicate no baseline.
	 */
	private Long baseline;

	/** The end commit used for TIA and for uploading the coverage. May not be null. */
	private CommitDescriptor endCommit;

	/** The URLs (including port) at which the agents listen. May be empty but not null. */
	private List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis = Collections.emptyList();

	/** The directory used to store test-wise coverage reports. Must be a writeable directory. */
	private File reportDirectory;

	/** @see #runImpacted */
	private boolean isRunImpacted() {
		return runImpacted;
	}

	/** @see #runAllTests */
	private boolean isRunAllTests() {
		return runAllTests;
	}

	/** @see #reportDirectory */
	public File getReportDirectory() {
		return reportDirectory;
	}

	/** Returns the builder for {@link TestEngineOptions}. */
	public static Builder builder() {
		return new Builder();
	}

	public ITestExecutor createTestExecutor() {
		if (!isRunImpacted()) {
			return new DelegatingTestExecutor();
		}
		if (isRunAllTests()) {
			return new TestWiseCoverageCollectingTestExecutor(testwiseCoverageAgentApis);
		}

		return new ImpactedTestsExecutor(testwiseCoverageAgentApis, serverOptions, baseline, endCommit, partition);
	}

	/** The builder for {@link TestEngineOptions}. */
	public static class Builder {

		private TestEngineOptions testEngineOptions = new TestEngineOptions();

		private Builder() {
		}

		/** @see #serverOptions */
		public Builder serverOptions(ServerOptions serverOptions) {
			testEngineOptions.serverOptions = serverOptions;
			return this;
		}

		/** @see #partition */
		public Builder partition(String partition) {
			testEngineOptions.partition = partition;
			return this;
		}

		/** @see #runAllTests */
		public Builder runImpacted(boolean runImpacted) {
			testEngineOptions.runImpacted = runImpacted;
			return this;
		}

		/** @see #runAllTests */
		public Builder runAllTests(boolean runAllTests) {
			testEngineOptions.runAllTests = runAllTests;
			return this;
		}

		/** @see #endCommit */
		public Builder endCommit(CommitDescriptor endCommit) {
			testEngineOptions.endCommit = endCommit;
			return this;
		}

		/** @see #baseline */
		public Builder baseline(Long baseline) {
			testEngineOptions.baseline = baseline;
			return this;
		}

		/** @see #testwiseCoverageAgentApis */
		public Builder agentUrls(List<String> agentUrls) {
			testEngineOptions.testwiseCoverageAgentApis = agentUrls.stream()
					.map(HttpUrl::parse)
					.map(ITestwiseCoverageAgentApi::createService)
					.collect(Collectors.toList());
			return this;
		}

		/** @see #reportDirectory */
		public Builder reportDirectory(String reportDirectory) {
			testEngineOptions.reportDirectory = new File(reportDirectory);
			return this;
		}

		/** Checks field conditions and returns the built {@link TestEngineOptions}. */
		public TestEngineOptions build() {
			Preconditions.notNull(testEngineOptions.endCommit, "End commit must be set.");
			Preconditions.notNull(testEngineOptions.serverOptions, "Server options must be set.");
			Preconditions.notNull(testEngineOptions.testwiseCoverageAgentApis, "Agent urls may be empty but not null.");
			Preconditions.notNull(testEngineOptions.reportDirectory, "Report directory must be set.");
			Preconditions.condition(
					testEngineOptions.reportDirectory.isDirectory() && testEngineOptions.reportDirectory.canWrite(),
					"Report directory must be readable directory: " + testEngineOptions.reportDirectory);
			return testEngineOptions;
		}
	}
}
