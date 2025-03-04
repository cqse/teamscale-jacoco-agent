package com.teamscale.aggregation.junit

import org.gradle.api.tasks.Sync

/** Task used to collect JUnit reports. This is a subclass of [Sync] to act as a marker in the [TeamscaleUpload] task. */
abstract class JUnitReportCollectionTask: Sync()
