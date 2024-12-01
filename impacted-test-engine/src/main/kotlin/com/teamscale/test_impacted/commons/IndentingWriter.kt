package com.teamscale.test_impacted.commons

/** Utility class for writing lines with tab indentation.  */
class IndentingWriter {
	private val builder = StringBuilder()

	private var indent = 0

	/** Indents all [writeLine] calls in the indented writes by one more tab.  */
	fun indent(indentedWrites: Runnable) {
		indent++
		indentedWrites.run()
		indent--
	}

	/** Writes a new line.  */
	fun writeLine(line: String) {
		builder.append("\t".repeat(indent))
			.append(line)
			.append("\n")
	}

	override fun toString() = builder.toString()
}
