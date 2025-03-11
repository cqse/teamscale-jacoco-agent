package com.teamscale.test.commons;

/** Holds a single report uploaded to our fake Teamscale server. */
public class ExternalReport {
	private final String reportString;

	public ExternalReport(String reportString) {
		this.reportString = reportString;
	}

	public String getReportString() {
		return reportString;
	}
}
