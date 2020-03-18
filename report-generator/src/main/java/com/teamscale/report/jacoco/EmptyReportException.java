package com.teamscale.report.jacoco;

/**
 * Exception indicating that the generated report was empty and no {@link CoverageFile} was written to disk.
 */
public class EmptyReportException extends Exception {

	public EmptyReportException() {
	}

	public EmptyReportException(String s) {
		super(s);
	}

	public EmptyReportException(String s, Throwable throwable) {
		super(s, throwable);
	}

	public EmptyReportException(Throwable throwable) {
		super(throwable);
	}

	protected EmptyReportException(String s, Throwable throwable, boolean b, boolean b1) {
		super(s, throwable, b, b1);
	}
}
