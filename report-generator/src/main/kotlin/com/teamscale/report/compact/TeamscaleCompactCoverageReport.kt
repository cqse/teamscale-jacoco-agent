package com.teamscale.report.compact

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.teamscale.report.util.CompactLines
import com.teamscale.report.util.LineRangeDeserializer
import com.teamscale.report.util.LineRangeSerializer
import java.io.OutputStream

/**
 * Teamscale Compact Coverage report on a per-file based granularity to reduce the amount of data
 * sent to and processed by Teamscale.
 *
 * @param version Version number for the Teamscale Compact Coverage report. Default is version 1.
 * @param coverage Coverage information on an aggregated per-file granularity.
 */
data class TeamscaleCompactCoverageReport @JsonCreator constructor(
	@JsonProperty("version") val version: Int,
	@JsonProperty("coverage") val coverage: List<CompactCoverageFileInfo>
) {
	/** Serializes the compact coverage into a JSON report and writes it into the given output stream. */
	fun writeTo(output: OutputStream) {
		return ObjectMapper()
			.setSerializationInclusion(JsonInclude.Include.NON_NULL)
			.writeValue(output, this)
	}

	/**
	 * Class that describes the coverage data for a single file. All list of lines are represented by
	 * strings possibly including ranges (denoted by '-').
	 */
	data class CompactCoverageFileInfo @JsonCreator constructor(
		@JsonProperty("filePath") val filePath: String,
		@JsonProperty("fullyCoveredLines")
		@JsonSerialize(using = LineRangeSerializer::class)
		@JsonDeserialize(using = LineRangeDeserializer::class)
		val fullyCoveredLines: CompactLines = CompactLines(),

		@JsonProperty("partiallyCoveredLines")
		@JsonSerialize(using = LineRangeSerializer::class)
		@JsonDeserialize(using = LineRangeDeserializer::class)
		val partiallyCoveredLines: CompactLines? = CompactLines(),

		@JsonProperty("uncoveredLines")
		@JsonSerialize(using = LineRangeSerializer::class)
		@JsonDeserialize(using = LineRangeDeserializer::class)
		val uncoveredLines: CompactLines? = null
	)
}
