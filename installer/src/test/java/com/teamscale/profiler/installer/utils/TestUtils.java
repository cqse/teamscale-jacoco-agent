package com.teamscale.profiler.installer.utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;

import static org.assertj.core.api.Assertions.assertThat;

/** Generic utilities for tests. */
public class TestUtils {

	/**
	 * Changes the given path to read-only and asserts that the change was successful. Depending on the operating
	 * system, this may or may not also mark subpaths as read-only, so do not rely on this.
	 */
	public static void makePathReadOnly(Path path) throws IOException {
		if (SystemUtils.IS_OS_WINDOWS) {
			// File#setWritable doesn't work under Windows 11 (always returns false).
			// So we manually set the readonly attribute. Unlike under Linux, this only prevent deletion of this
			// specific path, not its subpaths
			DosFileAttributeView dosAttributes = Files.getFileAttributeView(path, DosFileAttributeView.class);
			dosAttributes.setReadOnly(true);
		} else {
			assertThat(path.toFile().setWritable(false, false))
					.withFailMessage("Failed to mark " + path + " as writable = " + false).isTrue();
		}
	}
}
