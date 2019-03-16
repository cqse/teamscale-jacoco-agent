package com.teamscale.test_impacted.engine.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.test_impacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.test_impacted.engine.ImpactedTestEngine;
import com.teamscale.test_impacted.engine.ImpactedTestEngineConfiguration;
import com.teamscale.test_impacted.engine.TestEngineRegistry;
import com.teamscale.test_impacted.engine.executor.DelegatingTestExecutor;
import com.teamscale.test_impacted.engine.executor.ITestExecutor;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsExecutor;
import com.teamscale.test_impacted.engine.executor.TestwiseCoverageCollectingTestExecutor;
import okhttp3.HttpUrl;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestEngine;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

	/** The test engine ids of all {@link TestEngine}s to use. If empty all available {@link TestEngine}s are used. */
	private Set<String> testEngineIds = Collections.emptySet();

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

	public ImpactedTestEngineConfiguration createTestEngineConfiguration() {
		ITestExecutor testExecutor = createTestExecutor();
		TestEngineRegistry testEngineRegistry = new TestEngineRegistry(testEngineIds);

		return new ImpactedTestEngineConfiguration(reportDirectory, testEngineRegistry, testExecutor);
	}

	private ITestExecutor createTestExecutor() {
		if (!isRunImpacted()) {
			return new DelegatingTestExecutor();
		}
		if (isRunAllTests()) {
			return new TestwiseCoverageCollectingTestExecutor(testwiseCoverageAgentApis);
		}

		return new ImpactedTestsExecutor(testwiseCoverageAgentApis, serverOptions, baseline, endCommit, partition);
	}

	/** Returns the builder for {@link TestEngineOptions}. */
	public static Builder builder() {
		return new Builder();
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

		/** @see #testEngineIds */
		public Builder testEngineIds(List<String> testEngineIds) {
			testEngineOptions.testEngineIds = new HashSet<>(testEngineIds);
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
