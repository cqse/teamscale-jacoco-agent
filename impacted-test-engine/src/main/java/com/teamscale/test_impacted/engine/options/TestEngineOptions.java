package com.teamscale.test_impacted.engine.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.ServerConfiguration;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.test_impacted.engine.ImpactedTestEngine;
import com.teamscale.test_impacted.engine.ImpactedTestEngineConfiguration;
import com.teamscale.test_impacted.engine.TestDataWriter;
import com.teamscale.test_impacted.engine.TestEngineRegistry;
import com.teamscale.test_impacted.engine.executor.ITestSorter;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsSorter;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsProvider;
import com.teamscale.test_impacted.engine.executor.NOPTestSorter;
import com.teamscale.test_impacted.engine.executor.TeamscaleAgentNotifier;
import com.teamscale.tia.client.ITestwiseCoverageAgentApi;
import okhttp3.HttpUrl;
import org.junit.platform.engine.TestEngine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

	/** Executes only impacted tests, not all ones if set. Defaults to true. */
	private boolean runImpacted = true;

	/** Includes added tests in the list of tests to execute. Defaults to true */
	private boolean includeAddedTests = true;

	/** Includes failed and skipped tests in the list of tests to execute. Defaults to true */
	private boolean includeFailedAndSkipped = true;

	/**
	 * The baseline. Only code changes after the baseline are considered for determining impacted tests. May be null to
	 * indicate no baseline.
	 */
	private String baseline;

	/** The end commit used for TIA and for uploading the coverage. May not be null. */
	private CommitDescriptor endCommit;

	/** The URLs (including port) at which the agents listen. May be empty but not null. */
	private List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis = Collections.emptyList();

	/** The test engine ids of all {@link TestEngine}s to use. If empty all available {@link TestEngine}s are used. */
	private Set<String> includedTestEngineIds = Collections.emptySet();

	/** The test engine ids of all {@link TestEngine}s to exclude. */
	private Set<String> excludedTestEngineIds = Collections.emptySet();

	/** The directory used to store test-wise coverage reports. Must be a writeable directory. */
	private File reportDirectory;

	/** @see #runAllTests */
	private boolean isRunAllTests() {
		return runAllTests;
	}

	/** @see #includeAddedTests */
	private boolean isIncludeAddedTests() {
		return includeAddedTests;
	}

	/** @see #includeFailedAndSkipped */
	private boolean isIncludeFailedAndSkipped() {
		return includeFailedAndSkipped;
	}

	/** @see #partition */
	public String getPartition() {
		return partition;
	}

	public ImpactedTestEngineConfiguration createTestEngineConfiguration() {
		ITestSorter testSorter = createTestSorter();
		TeamscaleAgentNotifier teamscaleAgentNotifier = createTeamscaleAgentNotifier();
		TestEngineRegistry testEngineRegistry = new TestEngineRegistry(includedTestEngineIds, excludedTestEngineIds);
		TestDataWriter testDataWriter = new TestDataWriter(reportDirectory);

		return new ImpactedTestEngineConfiguration(testDataWriter, testEngineRegistry, testSorter, teamscaleAgentNotifier);
	}

	private ITestSorter createTestSorter() {
		if (!runImpacted) {
			return new NOPTestSorter();
		}

		ImpactedTestsProvider testsProvider = createImpactedTestsProvider();
		return new ImpactedTestsSorter(testsProvider);
	}

	private ImpactedTestsProvider createImpactedTestsProvider() {
		ServerConfiguration serverConfiguration = new ServerConfiguration(
				serverOptions.getUrl(),
				serverOptions.getUserName(),
				serverOptions.getUserAccessToken(),
				serverOptions.getProject()
		);
		TeamscaleClient client = new TeamscaleClient(
				serverConfiguration,
				new File(reportDirectory, "server-request.txt")
		);
		return new ImpactedTestsProvider(client, baseline, endCommit, partition,
				isRunAllTests(), isIncludeAddedTests(), isIncludeFailedAndSkipped());
	}

	private TeamscaleAgentNotifier createTeamscaleAgentNotifier() {
		return new TeamscaleAgentNotifier(testwiseCoverageAgentApis,
				runImpacted && !runAllTests);
	}

	/** Returns the builder for {@link TestEngineOptions}. */
	public static Builder builder() {
		return new Builder();
	}

	/** The builder for {@link TestEngineOptions}. */
	public static class Builder {

		private final TestEngineOptions testEngineOptions = new TestEngineOptions();

		private Builder() {
			// Only needed to make constructor private
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

		/** @see #runImpacted */
		public Builder runImpacted(boolean runImpacted) {
			testEngineOptions.runImpacted = runImpacted;
			return this;
		}

		/** @see #runAllTests */
		public Builder runAllTests(boolean runAllTests) {
			testEngineOptions.runAllTests = runAllTests;
			return this;
		}

		/** @see #includeAddedTests */
		public Builder includeAddedTests(boolean includeAddedTests) {
			testEngineOptions.includeAddedTests = includeAddedTests;
			return this;
		}

		/** @see #includeFailedAndSkipped */
		public Builder includeFailedAndSkipped(boolean includeFailedAndSkipped) {
			testEngineOptions.includeFailedAndSkipped = includeFailedAndSkipped;
			return this;
		}

		/** @see #endCommit */
		public Builder endCommit(CommitDescriptor endCommit) {
			testEngineOptions.endCommit = endCommit;
			return this;
		}

		/** @see #baseline */
		public Builder baseline(String baseline) {
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

		/** @see #includedTestEngineIds */
		public Builder includedTestEngineIds(List<String> testEngineIds) {
			testEngineOptions.includedTestEngineIds = new HashSet<>(testEngineIds);
			return this;
		}

		/** @see #excludedTestEngineIds */
		public Builder excludedTestEngineIds(List<String> testEngineIds) {
			testEngineOptions.excludedTestEngineIds = new HashSet<>(testEngineIds);
			return this;
		}

		/** @see #reportDirectory */
		public Builder reportDirectory(String reportDirectory) {
			if (reportDirectory != null) {
				testEngineOptions.reportDirectory = new File(reportDirectory);
			}
			return this;
		}

		/** Checks field conditions and returns the built {@link TestEngineOptions}. */
		public TestEngineOptions build() {
			TestEngineOptionUtils.assertNotNull(testEngineOptions.endCommit, "End commit must be set.");
			if (testEngineOptions.runImpacted) {
				TestEngineOptionUtils.assertNotNull(testEngineOptions.serverOptions, "Server options must be set.");
			}
			TestEngineOptionUtils.assertNotNull(testEngineOptions.testwiseCoverageAgentApis,
					"Agent urls may be empty but not null.");
			TestEngineOptionUtils.assertNotNull(testEngineOptions.reportDirectory, "Report directory must be set.");
			if (!testEngineOptions.reportDirectory.isDirectory() || !testEngineOptions.reportDirectory.canWrite()) {
				try {
					Files.createDirectories(testEngineOptions.reportDirectory.toPath());
				} catch (IOException e) {
					throw new AssertionError(
							"Report directory could not be created: " + testEngineOptions.reportDirectory, e);
				}
			}
			return testEngineOptions;
		}
	}
}
