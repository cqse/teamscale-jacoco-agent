package com.teamscale.client

/** Enum of report formats.  */
enum class EReportFormat private constructor(
    /** A readable name for the report type.  */
    val readableName: String
) {
    JACOCO("JaCoCo Coverage"),
    TESTWISE_COVERAGE("Testwise Coverage")
}
