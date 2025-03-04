import com.teamscale.TeamscaleUpload
import com.teamscale.aggregation.compact.AggregateCompactCoverageReport
import com.teamscale.aggregation.junit.AggregateJUnitReport
import com.teamscale.aggregation.testwise.AggregateTestwiseCoverageReport

plugins {
	id("com.teamscale.aggregation")
}

reporting {
	reports {
		val unitTestAggregateCompactCoverageReport by creating(AggregateCompactCoverageReport::class) {
			testSuiteName = SuiteNames.UNIT_TEST
		}
		val unitTestAggregateJUnitReport by creating(AggregateJUnitReport::class) {
			testSuiteName = SuiteNames.UNIT_TEST
		}
		val systemTestAggregateTestwiseCoverageReport by creating(AggregateTestwiseCoverageReport::class) {
			testSuiteName = SuiteNames.SYSTEM_TEST
		}
	}
}

tasks.register<TeamscaleUpload>("teamscaleTestReportUpload") {
	partition = "Default Tests"
	from(tasks.named("testAggregateCompactCoverageReport"))
	from(tasks.named("testAggregateJUnitReport"))
}

tasks.register<TeamscaleUpload>("teamscaleUnitTestReportUpload") {
	partition = "Unit Tests"
	from(tasks.named("unitTestAggregateCompactCoverageReport"))
	from(tasks.named("unitTestAggregateJUnitReport"))
}

tasks.register<TeamscaleUpload>("teamscaleSystemTestReportUpload") {
	partition = "System Tests"
	from(tasks.named("systemTestAggregateTestwiseCoverageReport"))
}
