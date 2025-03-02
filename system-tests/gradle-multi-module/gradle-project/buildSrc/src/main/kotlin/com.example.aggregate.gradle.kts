import com.teamscale.TeamscaleUpload
import com.teamscale.aggregation.compact.AggregateCompactCoverageReport

plugins {
	id("com.teamscale.aggregation")
}

reporting {
	reports {
		val unitTestAggregateCompactCoverageReport by creating(AggregateCompactCoverageReport::class) {
			testSuiteName = SuiteNames.UNIT_TEST
		}
	}
}

tasks.register<TeamscaleUpload>("teamscaleTestReportUpload") {
	partition = "Default Tests"
	from(tasks.named("testAggregateCompactCoverageReport"))
	aggregatedJUnitReportsFrom("test")
}

tasks.register<TeamscaleUpload>("teamscaleUnitTestReportUpload") {
	partition = "Unit Tests"
	from(tasks.named("unitTestAggregateCompactCoverageReport"))
	aggregatedJUnitReportsFrom(SuiteNames.UNIT_TEST)
}

tasks.register<TeamscaleUpload>("teamscaleSystemTestReportUpload") {
	partition = "System Tests"
	aggregatedTestwiseCoverageReportsFrom(SuiteNames.SYSTEM_TEST)
}
