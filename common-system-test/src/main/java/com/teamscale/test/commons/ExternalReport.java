package com.teamscale.test.commons;

/** Holds a single report that was uploaded to our fake Teamscale server. */
public class ExternalReport {
	private final String reportString;
	private final String partition;

	public ExternalReport(String reportString, String partition) {
		this.reportString = reportString;
		this.partition = partition;
	}

	public String getReportString() {
		return reportString;
	}

	public String getPartition() {
		return partition;
	}
}
