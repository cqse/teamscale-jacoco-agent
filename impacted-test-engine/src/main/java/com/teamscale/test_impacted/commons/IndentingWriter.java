package com.teamscale.test_impacted.commons;

/** Utility class for writing lines with tab indentation. */
public class IndentingWriter {

	private StringBuilder builder = new StringBuilder();

	private int indent = 0;

	/** Indents all {@link #writeLine(String)} calls in the indented writes by one more tab. */
	public void indent(Runnable indentedWrites) {
		indent++;
		indentedWrites.run();
		indent--;
	}

	/** Writes a new line. */
	public void writeLine(String line) {
		for (int i = 0; i < indent; i++) {
			builder.append("\t");
		}
		builder.append(line).append("\n");
	}

	@Override
	public String toString() {
		return builder.toString();
	}
}
