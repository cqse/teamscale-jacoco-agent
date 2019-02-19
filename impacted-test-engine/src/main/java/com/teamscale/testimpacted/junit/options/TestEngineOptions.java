package com.teamscale.testimpacted.junit.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.testimpacted.junit.ImpactedTestEngine;
import okhttp3.HttpUrl;
import org.junit.platform.commons.util.Preconditions;

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

	/** The baseline. Only code changes after the baseline are considered for determining impacted tests. May be null to indicate no baseline. */
	private Long baseline;

	/** The end commit used for TIA and for uploading the coverage. May not be null. */
	private CommitDescriptor endCommit;

	/** The URLs (including port) at which the agents listen. May be empty but not null. */
	private List<HttpUrl> agentsUrls = Collections.emptyList();

	/** @see #serverOptions */
	public ServerOptions getServerOptions() {
		return serverOptions;
	}

	/** @see #partition */
	public String getPartition() {
		return partition;
	}

	/** @see #runImpacted */
	public boolean isRunImpacted() {
		return runImpacted;
	}

	/** @see #runAllTests */
	public boolean isRunAllTests() {
		return runAllTests;
	}

	/** @see #endCommit */
	public CommitDescriptor getEndCommit() {
		return endCommit;
	}

	/** @see #baseline */
	public Long getBaseline() {
		return baseline;
	}

	/** @see #agentsUrls */
	public List<HttpUrl> getAgentsUrls() {
		return agentsUrls;
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

		/** @see #partition  */
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

		/** @see #agentsUrls */
		public Builder agentUrls(List<String> agentUrls) {
			testEngineOptions.agentsUrls = agentUrls.stream().map(HttpUrl::parse).collect(Collectors.toList());
			return this;
		}

		/** Checks field conditions and returns the built {@link TestEngineOptions}. */
		public TestEngineOptions build() {
			Preconditions.notNull(testEngineOptions.endCommit, "End commit must be set.");
			Preconditions.notNull(testEngineOptions.serverOptions, "Server options must be set.");
			Preconditions.notNull(testEngineOptions.agentsUrls, "Agent urls may be empty but not null.");
			return testEngineOptions;
		}
	}
}
