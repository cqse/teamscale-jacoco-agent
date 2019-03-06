package com.teamscale.testimpacted.commons;

public class IndentingWriter {

	private StringBuilder builder = new StringBuilder();

	private int indent = 0;

	public void indent(Runnable indentedWrites) {
		indent++;
		indentedWrites.run();
		indent--;
	}

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
