package com.teamscale.report.jacoco;

/**
 * Exception indicating that the generated report was empty and no {@link CoverageFile} was written to disk.
 */
public class EmptyReportException extends Exception {

	public EmptyReportException(String message) {
		super(message);
	}
}
