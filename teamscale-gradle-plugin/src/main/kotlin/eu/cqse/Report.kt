package eu.cqse

import eu.cqse.teamscale.client.EReportFormat
import java.io.File

data class Report(
        val format: EReportFormat,
        val report: File,
        var partition: String,
        var message: String
)