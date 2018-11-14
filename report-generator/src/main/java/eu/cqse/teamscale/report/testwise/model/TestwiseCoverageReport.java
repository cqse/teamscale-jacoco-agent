package eu.cqse.teamscale.report.testwise.model;

import java.util.ArrayList;
import java.util.List;

/** Container for coverage produced by multiple tests. */
public class TestwiseCoverageReport {

	/** The tests contained in the report. */
	public final List<TestInfo> tests = new ArrayList<>();

}
