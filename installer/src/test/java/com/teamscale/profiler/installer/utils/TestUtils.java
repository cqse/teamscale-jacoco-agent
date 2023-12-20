package com.teamscale.profiler.installer.utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;

import static org.assertj.core.api.Assertions.assertThat;

/** Generic utilities for tests. */
public class TestUtils {

	/** Changes the given path to read-only and asserts that the change was successful. */
	public static void makePathReadOnly(Path path) throws IOException {
		if (SystemUtils.IS_OS_WINDOWS) {
			DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
			view.setReadOnly(true);
		} else {
			assertThat(path.toFile().setWritable(false, false))
					.withFailMessage("Failed to mark " + path + " as writable = " + false).isTrue();
		}
	}
}
