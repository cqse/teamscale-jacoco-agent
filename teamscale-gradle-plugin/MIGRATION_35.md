# Migration guide for the `com.teamscale` Gradle plugin version 34 to 35
Previously, the plugin provided a way to specify the partitions either globally for a project or on a per task level.
Additionally, a `teamscaleReportUpload` task was provided automatically that would lazily pick up all the reports and upload them to Teamscale.

With this version, we make use of the Gradle configuration cache for a faster configuration phase.
Due to the restrictions that are imposed by the Gradle configuration cache, we had to adjust the API of the plugin.
Now you need to create the `teamscaleReportUpload` task explicitly (see below for details).
This gives you more control over which artifacts need to be uploaded together, and when in your build lifecycle, this is going to happen.

## Lazy properties
To avoid issues with plugin execution order, we switched all extension and task properties from plain types e.g. `boolean` to `Property<Boolean>` etc.
In all Gradle versions supported by the plugin (Gradle 8.4+) this is handled transparently.
For more details, please refer to the official documentation: [Lazy Configuration](https://docs.gradle.org/8.1.1/userguide/lazy_configuration.html#lazy_properties).

## The `teamscaleReportUpload` task
The `teamscaleReportUpload` task was previously provided automatically on the root project,
that would lazily pick up all the reports produced by any project during the current Gradle invocation
and upload them to Teamscale once all tasks were done.

Now you need to define your upload task(s) explicitly.
The information about the `partition`, `message` and reports that should be uploaded within that task also need to be specified explicitly.
The type of the upload task was renamed from `TeamscaleUploadTask` to `TeamscaleUpload`.

Most Gradle projects consist of multiple smaller subprojects.
Depending on how the tests for those projects are executed, you will want to implement a different upload strategy.
It's usually most efficient to only have one (or very few) uploads per CI machine, which executes a subset of the tests.
So if you are, e.g., running all unit tests for a large number of subprojects, you will want to aggregate the resulting coverage and test execution data into one upload.
An aggregated report is more efficient to generate, upload, store and process in Teamscale, compared to multiple small reports. 

### Upload with multi-project aggregation
We provide support for creating aggregated reports based on the Gradle [JVM Test Suite Plugin](https://docs.gradle.org/8.10.2/userguide/jvm_test_suite_plugin.html),
but you can also aggregate reports without running your tests via JVM test suites.

Let's assume you have a "integrationTest" test suite defined in your subprojects:
```groovy
testing {
    suites {
        integrationTest(JvmTestSuite) {
            // ...
        }
    }
}
```

Then apply the `com.teamscale.aggregation` plugin to project that should perform the aggregation.
This is usually the same project that also applies the `distribution`/`application` plugin,
but it can also be a separate project.
By default, all projects that end up on the runtime classpath of the project are used for the aggregation.
Projects can also be manually added
by adding the projects to the `jacocoAggregation` and `reportAggregation` configurations.

#### With JVM test suites
The `com.teamscale.aggregation` plugin automatically creates a <code><em>testSuite</em>AggregateCompactCoverageReport</code> task for each test suite in the presence of the JVM test suite plugin.
The task will collect coverage data across all projects
and generate a [Teamscale Compact Coverage](https://docs.teamscale.com/reference/upload-formats-and-samples/teamscale-compact-coverage/) report from it.

```groovy
plugins {
	id 'com.teamscale.aggregation'
}

tasks.register("teamscaleIntegrationTestReportUpload", TeamscaleUpload) {
	partition = "Integration Tests"
	from(tasks.integrationTestAggregateCompactCoverageReport)
	aggregatedJUnitReportsFrom("integrationTest")
}
```

`aggregatedJUnitReportsFrom(testSuiteName)` additionally configures the upload task to collect JUnit report from all dependant projects.  

#### Without JVM test suites
If your projects do not yet use JVM test suites, you can attach a testSuiteName to arbitrary tasks.
For this purpose we provide the `TestSuiteCompatibilityUtil.exposeTestForAggregation(testTask, suiteName)`.
Call this for each task in each subproject that you want to take part in aggregation.

```groovy
import com.teamscale.aggregation.TestSuiteCompatibilityUtil

tasks.register('unitTest', Test) {
    // ...
}

TestSuiteCompatibilityUtil.exposeTestForAggregation(tasks.named('unitTest'), 'myUnitTestSuite')
```

In the aggregation project create an aggregation report:
```groovy
import com.teamscale.aggregation.compact.AggregateCompactCoverageReport

plugins {
	id 'com.teamscale.aggregation'
}

reporting {
    reports {
        unitTestAggregateCompactCoverageReport(AggregateCompactCoverageReport) { 
            testSuiteName = 'myUnitTestSuite'
        }
    }
}

tasks.register("teamscaleUnitTestReportUpload", TeamscaleUpload) {
	partition = "Unit Tests"
	from(tasks.named('unitTestAggregateCompactCoverageReport'))
	aggregatedJUnitReportsFrom('myUnitTestSuite')
}
```

This approach also works for `TestImpacted` tasks.
`aggregatedTestwiseCoverageReportsFrom(testSuiteName)` allows the TeamscaleUpload task to pick up those reports.

### Upload without aggregation
For each project that you want to individually upload data from, you can create one or multiple `TeamscaleUpload` tasks.

```groovy
import com.teamscale.TeamscaleUpload
// ...

tasks.register("teamscaleReportUpload", TeamscaleUpload) {
    partition = "Unit Tests"
    message = "Gradle upload" // Optional
    // If you don't collect testwise coverage
    from(tasks.test) // Will upload the JUnit reports produced by the Test task
    from(tasks.jacocoTestReport) // Will upload the coverage report produced by the JacocoReport task
    
    // or if you do collect testwise coverage
    from(tasks.tiaTests) // Will upload the Testwise Coverage report produced by the TestImpacted task
}
```

The `from` method accepts `Test`, `TestImpacted`, `JacocoReport` or `CompactCoverageReport` tasks,
whose produced reports will be uploaded.
Additionally, can also use `addReport(format, files)` to attach other report types to the upload e.g.,
Spotbugs reports or reports produced by other custom tasks.


## Specifying partitions top-level
The `teamscale.report` extension action was removed.
This was previously used to specify the `partition` and `message` to use during the report upload for all tasks of a project.
The `partition` and `message` are now specified via the `TeamscaleUpload` task, see above.

When you use test impact analysis, you need to additionally replace `teamscale.report.testwiseCoverage`
```groovy
teamscale {
    report {
        testwiseCoverage {
            partition = "Unit Tests"
        }
    }
}
```
with one of the following:
```groovy
import com.teamscale.TestImpacted
// ...
tasks.withType(TestImpacted) {
    partition = "Unit Tests"
}
```
This needs to be specified only for the TestImpacted task as it needs to request impacted tests from Teamscale before the tests are executed.

## `TestwiseCoverageReportTask` was merged into `TestImpacted`
The testwise coverage report is now generated directly by the corresponding `TestImpacted` task (same like the native Gradle test result reports `junitXml` and `html`).

```groovy
import com.teamscale.TestImpacted
// ...

tasks.register("tiaUnitTests", TestImpacted) {
    reports.testwiseCoverage.required = true // Previously called collectTestwiseCoverage, default is true
    reports.testwiseCoverage.outputLocation = layout.buildDirectory.file("reports/tiaUnitTests/testwise-coverage.json")
}
```
