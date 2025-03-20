package com.teamscale.test.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.teamscale.client.EReportFormat;
import com.teamscale.client.JsonUtils;
import com.teamscale.report.compact.TeamscaleCompactCoverageReport;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import org.conqat.lib.commons.collections.ListMap;
import spark.Request;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an upload session to Teamscale which contains data from a single partition, for a specific
 * commit/revision. A session may contain data in multiple formats.
 */
public class Session {

	private final String partition;
	private final String revision;
	private final String commit;
	private final String repository;
	private boolean committed;

	private final ListMap<EReportFormat, ExternalReport> reports = new ListMap<>();

	public Session(Request request) {
		partition = request.queryParams("partition");
		revision = request.queryParams("revision");
		commit = request.queryParams("t");
		repository = request.queryParams("repository");
	}

	/**
	 * Retrieves the commit information for the session. Combines the revision and commit data into a single string
	 * representation separated by a comma.
	 */
	public String getCommit() {
		return revision + ":" + repository + ", " + commit;
	}

	/** Returns the partition name for which the session was opened. */
	public String getPartition() {
		return partition;
	}

	/**
	 * Marks the session as committed, which means no more data will be added, and a real Teamscale would start
	 * processing the data now.
	 */
	public void markCommitted() {
		committed = true;
	}

	/** Whether the session was commited. */
	public boolean isCommitted() {
		return committed;
	}

	/** Adds a new report into the session. */
	public void addReport(String format, String report) {
		reports.add(EReportFormat.valueOf(format), new ExternalReport(report));
	}

	/** Returns all reports form this session. */
	public List<ExternalReport> getReports() {
		return reports.getValues();
	}

	/** Returns all reports of the given format. */
	public List<ExternalReport> getReports(EReportFormat format) {
		return reports.getCollection(format);
	}

	/** Returns the only report in the given format. It asserts that there are no other reports present. */
	public String getOnlyReport(EReportFormat format) {
		if (reports.getKeys().size() != 1) {
			throw new AssertionError("Expected exactly one report format, but got " + reports.getKeys() + "!");
		}
		if (!reports.containsCollection(format)) {
			throw new AssertionError(
					"No " + format.getReadableName() + " report found! Session contains " + reports.getKeys()
							.stream().map(EReportFormat::getReadableName)
							.collect(Collectors.toSet()) + " reports.");
		}
		if (reports.getCollection(format).size() != 1) {
			throw new AssertionError(
					"Expected exactly one " + format.getReadableName() + " report, but got " +
							reports.getCollection(format).size() + ".");
		}
		return reports.getCollection(format).get(0).getReportString();
	}

	/**
	 * Returns the report at the given index in {@link #reports}, parsed as a {@link TestwiseCoverageReport}.
	 *
	 * @throws IOException when parsing the report fails.
	 */
	public TestwiseCoverageReport getTestwiseCoverageReport(int index) throws IOException {
		return JsonUtils.deserialize(
				reports.getCollection(EReportFormat.TESTWISE_COVERAGE).get(index).getReportString(),
				TestwiseCoverageReport.class);
	}

	/** Returns the only Testwise Coverage report in deserialized form. */
	public TestwiseCoverageReport getOnlyTestwiseCoverageReport() throws JsonProcessingException {
		return JsonUtils.deserialize(
				getOnlyReport(EReportFormat.TESTWISE_COVERAGE),
				TestwiseCoverageReport.class);
	}

	/** Returns the only Compact Coverage report in deserialized form. */
	public TeamscaleCompactCoverageReport getOnlyCompactCoverageReport() throws JsonProcessingException {
		return JsonUtils.deserialize(
				getOnlyReport(EReportFormat.TEAMSCALE_COMPACT_COVERAGE),
				TeamscaleCompactCoverageReport.class);
	}

	/**
	 * Returns the report at the given index in {@link #reports}, parsed as a {@link TeamscaleCompactCoverageReport}.
	 *
	 * @throws IOException when parsing the report fails.
	 */
	public TeamscaleCompactCoverageReport getCompactCoverageReport(int index) throws IOException {
		return JsonUtils.deserialize(
				reports.getCollection(EReportFormat.TEAMSCALE_COMPACT_COVERAGE).get(index).getReportString(),
				TeamscaleCompactCoverageReport.class);
	}
}
