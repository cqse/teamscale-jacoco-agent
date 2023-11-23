package com.teamscale.report.testwise.model;

/** Holds coverage of a single file. */
public class FileCoverage {

	/** The name of the file. */
	public final String fileName;

	/** A list of line ranges that have been covered. */
	public final String coveredLines;

	@SuppressWarnings("unused")
		// Moshi might use this (TS-36477)
	FileCoverage() {
		this("", "");
	}

	public FileCoverage(String fileName, String coveredLines) {
		this.fileName = fileName;
		this.coveredLines = coveredLines;
	}
}
