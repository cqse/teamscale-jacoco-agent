package com.teamscale.report.jacoco;

import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Represents a coverage file on disk. The main purpose is to avoid reading the entire file into memory as this
 * dramatically increases the memory footprint of the JVM which might run out of memory because of this. Therefore
 * please avoid using the {@link CoverageFile#getFile()} method to read the entire file
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
	 * Delete the coverage file from disk
	 *
	 * @return whether the operation was successful or not
	 */
	public boolean delete() {
		return coverageFile.delete();
	}

	/**
	 * Returns the underlying {@link java.io.File} object. Make sure to avoid reading all of it into memory!
	 */
	public File getFile() {
		return coverageFile;
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
