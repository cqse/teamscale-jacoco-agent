import com.teamscale.TeamscaleUpload

plugins {
	java
	id("com.teamscale")
}

val testwiseCoverageReports by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.getByName("implementation"))
    attributes {
		attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.VERIFICATION))
		attribute(Usage.USAGE_ATTRIBUTE, objects.named("testwise-coverage"))
    }
}

tasks.register<TeamscaleUpload>("teamscaleReportUpload") {
	partition = "Unit Tests"
	from(tasks.named("tiaTests"))
	addReport("TESTWISE_COVERAGE", testwiseCoverageReports.incoming.artifactView { lenient(true) }.files)
}
