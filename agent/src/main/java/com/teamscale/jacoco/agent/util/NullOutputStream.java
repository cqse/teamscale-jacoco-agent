package com.teamscale.jacoco.agent.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

/** NOP output stream implementation. */
public class NullOutputStream extends OutputStream {

	public NullOutputStream() {
		// do nothing
	}

	@Override
	public void write(final byte @NotNull [] b, final int off, final int len) {
		// to /dev/null
	}

	@Override
	public void write(final int b) {
		// to /dev/null
	}

	@Override
	public void write(final byte @NotNull [] b) throws IOException {
		// to /dev/null
	}
}
