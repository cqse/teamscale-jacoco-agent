# Migration guide for the `com.teamscale` Gradle plugin version 34 to 35
Previously, the plugin provided a way to specify the partitions either globally for a project or on a per task level.
Additionally, a `teamscaleReportUpload` task was provided automatically that would lazily pick up all the reports and upload them to Teamscale.

With this version, we make use of the Gradle configuration cache for a faster configuration phase.
Due to the restrictions that are imposed by the Gradle configuration cache, we had to adjust the API of the plugin.
Now you need to create the `teamscaleReportUpload` task explicitly (see below for details).
This gives you more control over which artifacts need to be uploaded together, and when in your build lifecycle, this is going to happen.

## Lazy properties
To follow Gradle's best practices we switched all extension and task properties from plain types e.g. `boolean` to `Property<Boolean>` etc.
In all Gradle versions supported by the plugin (Gradle 8.4+) this is handled transparently.
For more details, please refer to the official documentation: [Lazy Configuration](https://docs.gradle.org/8.1.1/userguide/lazy_configuration.html#lazy_properties).

## The `teamscaleReportUpload` task
The `teamscaleReportUpload` task was previously provided automatically on the root project,
that would lazily pick up all the reports produced by any project during the current Gradle invocation
and upload them to Teamscale once all tasks were done.

Now you need to create the `teamscaleReportUpload` task explicitly.
The information about the `partition`, `message` and reports that should be uploaded with the task also need to be specified explicitly.
Instead, you need to define your own upload task for each partition.
The type of the upload task was renamed from `TeamscaleUploadTask` to `TeamscaleUpload`.

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

TODO aggregate at top-level

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
