package com.teamscale.report.jacoco;

import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a coverage file on disk. The main purpose is to avoid reading the entire file into memory as this
 * dramatically increases the memory footprint of the JVM which might run out of memory because of this. Therefore,
 * please avoid using the {@link CoverageFile#getAbsolutePath()} method to read the entire file
 */
public class CoverageFile {
	private File coverageFile;

	public CoverageFile(File coverageFile) {
		this.coverageFile = coverageFile;
	}

	/**
	 * Copies the coverage File in blocks from the disk to the output stream to avoid having to read the entire file
	 * into memory.
	 */
	public void copy(OutputStream outputStream) throws IOException {
		FileInputStream inputStream = new FileInputStream(coverageFile);
		FileSystemUtils.copy(inputStream, outputStream);
	}

	/**
	 * Get the filename of the coverage file on disk without its extension
	 */
	public String getNameWithoutExtension() {
		return FileSystemUtils.getFilenameWithoutExtension(coverageFile);
	}

	/**
	 * Get the {@link java.io.File} representing the parent direcotry of the coverage file
	 */
	public Path getParentDirectoryPath() {
		return coverageFile.getParentFile().toPath();
	}

	/**
	 * Get the absolute path of the underlying {@link java.io.File}. Do only use it for logging purposes and especially
	 * don't use it to read the entire file into memory.
	 */
	public String getAbsolutePath() {
		return coverageFile.getAbsolutePath();
	}

	/**
	 * Delete the coverage file from disk
	 */
	public void delete() throws IOException {
		Files.delete(coverageFile.toPath());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CoverageFile that = (CoverageFile) o;
		return coverageFile.equals(that.coverageFile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(coverageFile);
	}
}
