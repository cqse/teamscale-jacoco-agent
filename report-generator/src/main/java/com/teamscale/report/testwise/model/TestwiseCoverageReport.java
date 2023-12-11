package com.teamscale.report.testwise.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** Container for coverage produced by multiple tests. */
public class TestwiseCoverageReport {

	/**
	 * If set to `true` the set of tests contained in the report don't represent the full set of tests within a
	 * partition. These tests are added or updated in Teamscale, but no tests or executable units that are missing in
	 * the report will be deleted.
	 */
	public final boolean partial;

	/** The tests contained in the report. */
	public final List<TestInfo> tests = new ArrayList<>();

	@JsonCreator
	public TestwiseCoverageReport(@JsonProperty("partial") boolean partial) {
		this.partial = partial;
	}
}
