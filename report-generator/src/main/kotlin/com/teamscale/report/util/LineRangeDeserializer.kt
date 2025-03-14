package com.teamscale.report.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.io.IOException

/**
 * Custom Serializer to serialize [CompactLines] to string separated line ranges. See
 * [LineRangeStringParser] for more details.
 */
class LineRangeDeserializer : StdDeserializer<CompactLines>(CompactLines::class.java) {
	/**
	 * Constructs a `CompactLines` instance from a string representation of line number ranges.
	 * Ranges in the string are denoted with '-', e.g., * "1-5,8,11-13" as e.g., used for the Testwise
	 * Coverage report format.
	 */
	@Throws(IOException::class)
	override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): CompactLines {
		return LineRangeStringParser().parse(jsonParser.valueAsString)
	}

	companion object {
		private const val serialVersionUID = 1L
	}
}
