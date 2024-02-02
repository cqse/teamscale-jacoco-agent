package com.teamscale.report.testwise.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Holds coverage of a single file. */
public class FileCoverage {

	/** The name of the file. */
	public final String fileName;

	/** A list of line ranges that have been covered. */
	public final String coveredLines;

	@JsonCreator
	public FileCoverage(@JsonProperty("fileName") String fileName, @JsonProperty("coveredLines") String coveredLines) {
		this.fileName = fileName;
		this.coveredLines = coveredLines;
	}
}
