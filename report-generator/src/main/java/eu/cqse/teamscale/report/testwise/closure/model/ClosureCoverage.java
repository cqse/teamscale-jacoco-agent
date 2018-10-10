package eu.cqse.teamscale.report.testwise.closure.model;

import java.util.List;

/** Model for the google closure coverage format. */
public class ClosureCoverage {

	/** External ID of the test that produced the coverage. */
	public String uniformPath;

	/** Holds a list of all covered js files with absolute path. */
	public List<String> fileNames;

	/**
	 * Holds for each line in each file a list with true or null
	 * to indicate if the line has been executed.
	 */
	public List<List<Boolean>> executedLines;
}
