package com.teamscale.report.jacoco;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Represents a coverage file on disk. The main purpose is to avoid reading the entire file into memory as this
 * dramatically increases the memory footprint of the JVM which might run out of memory because of this.
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
	 */
	public void delete() throws IOException {
		Files.delete(coverageFile.toPath());
	}

	/**
	 * Create a {@link okhttp3.MultipartBody} form body with the contents of the coverage file.
	 */
	public RequestBody createFormRequestBody() {
		return RequestBody.create(MultipartBody.FORM, new File(coverageFile.getAbsolutePath()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return coverageFile.getAbsolutePath();
	}
}
