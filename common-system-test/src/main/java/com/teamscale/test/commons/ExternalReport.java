package com.teamscale.test.commons;

/** Holds a single report that was uploaded to our fake Teamscale server. */
public class ExternalReport {
	private final String reportString;
	private final String partition;
	private final String repositoryId;

	public ExternalReport(String reportString, String partition, String repositoryId) {
		this.reportString = reportString;
		this.partition = partition;
		this.repositoryId = repositoryId;
	}

	public String getReportString() {
		return reportString;
	}

	public String getPartition() {
		return partition;
	}

	public String getRepositoryId() {
		return repositoryId;
	}
}
