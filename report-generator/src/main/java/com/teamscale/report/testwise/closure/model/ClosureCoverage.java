package com.teamscale.report.testwise.closure.model;

import java.util.List;

/** Model for the google closure coverage format. */
public class ClosureCoverage {

	/** The uniformPath of the test that produced the coverage. */
	public final String uniformPath;

	/** Holds a list of all covered js files with absolute path. */
	public final List<String> fileNames;

	/**
	 * Holds for each line in each file a list with true or null
	 * to indicate if the line has been executed.
	 */
	public final List<List<Boolean>> executedLines;

	public ClosureCoverage(String uniformPath, List<String> fileNames, List<List<Boolean>> executedLines) {
		this.uniformPath = uniformPath;
		this.fileNames = fileNames;
		this.executedLines = executedLines;
	}
}
