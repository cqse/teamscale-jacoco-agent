package com.teamscale.test.commons

import com.fasterxml.jackson.core.JsonProcessingException
import com.teamscale.client.EReportFormat
import com.teamscale.client.JsonUtils
import com.teamscale.report.compact.TeamscaleCompactCoverageReport
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import org.conqat.lib.commons.collections.ListMap
import spark.Request
import java.io.IOException
import java.util.stream.Collectors

/**
 * Represents an upload session to Teamscale, which contains data from a single partition, for a specific
 * commit/revision. A session may contain data in multiple formats.
 */
class Session(request: Request) {
	/** Returns the partition name for which the session was opened.  */
	val partition: String? = request.queryParams("partition")
	private val revision: String? = request.queryParams("revision")
	private val commit: String? = request.queryParams("t")
	private val repository: String? = request.queryParams("repository")

	/** Whether the session was committed.  */
	var isCommitted: Boolean = false
		private set

	private val reports = ListMap<EReportFormat, ExternalReport>()

	/**
	 * Retrieves the commit information for the session. Combines the revision and commit data into a single string
	 * representation separated by a comma.
	 */
	fun getCommit() = "$revision:$repository, $commit"

	/**
	 * Marks the session as committed, which means no more data will be added, and a real Teamscale would start
	 * processing the data now.
	 */
	fun markCommitted() {
		isCommitted = true
	}

	/** Adds a new report into the session.  */
	fun addReport(format: String, report: String) {
		reports.add(EReportFormat.valueOf(format), ExternalReport(report))
	}

	/** Returns all reports from this session.  */
	fun getReports(): List<ExternalReport> = reports.getValues()

	/** Returns all reports of the given format.  */
	fun getReports(format: EReportFormat): MutableList<ExternalReport> = reports.getCollection(format)

	/** Returns the only report in the given format. It asserts that there are no other reports present.  */
	fun getOnlyReport(format: EReportFormat): String {
		check(reports.getKeys().size == 1) {
			"Expected exactly one report format, but got ${reports.getKeys()}!"
		}
		check(reports.containsCollection(format)) {
			"No ${format.readableName} report found! Session contains ${
				reports.getKeys().map(EReportFormat::readableName).toSet()
			} reports."
		}
		check(reports.getCollection(format).size == 1) {
			"Expected exactly one ${format.readableName} report, but got ${reports.getCollection(format).size}."
		}
		return reports.getCollection(format).first().reportString
	}

	/**
	 * Returns the report at the given index in [reports], parsed as a [TestwiseCoverageReport].
	 *
	 * @throws IOException when parsing the report fails.
	 */
	@Throws(IOException::class)
	fun getTestwiseCoverageReport(index: Int) =
		reports.getCollection(EReportFormat.TESTWISE_COVERAGE).getOrNull(index)?.let {
			JsonUtils.deserialize<TestwiseCoverageReport>(it.reportString)
		}

	@get:Throws(JsonProcessingException::class)
	val onlyTestwiseCoverageReport: TestwiseCoverageReport
		/** Returns the only Testwise Coverage report in deserialized form.  */
		get() = JsonUtils.deserialize<TestwiseCoverageReport>(
			getOnlyReport(EReportFormat.TESTWISE_COVERAGE)
		)

	@get:Throws(JsonProcessingException::class)
	val onlyCompactCoverageReport: TeamscaleCompactCoverageReport
		/** Returns the only Compact Coverage report in deserialized form.  */
		get() = JsonUtils.deserialize<TeamscaleCompactCoverageReport>(
			getOnlyReport(EReportFormat.TEAMSCALE_COMPACT_COVERAGE)
		)

	/**
	 * Returns the report at the given index in [reports], parsed as a [TeamscaleCompactCoverageReport].
	 *
	 * @throws IOException when parsing the report fails.
	 */
	@Throws(IOException::class)
	fun getCompactCoverageReport(index: Int) =
		reports.getCollection(EReportFormat.TEAMSCALE_COMPACT_COVERAGE).getOrNull(index)?.let {
			JsonUtils.deserialize<TeamscaleCompactCoverageReport>(it.reportString)
		}
}
