package com.teamscale.profiler.installer.utils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Generic utilities for tests. */
public class TestUtils {

	/** Changes the writability of the given path and asserts that the change was successful. */
	public static void setPathWritable(Path path, boolean shouldBeWritable) {
		assertThat(path.toFile().setWritable(shouldBeWritable, shouldBeWritable))
				.withFailMessage("Failed to mark " + path + " as writable = " + shouldBeWritable).isTrue();
	}
}
