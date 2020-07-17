package com.teamscale.report.jacoco;

import com.teamscale.client.FileSystemUtils;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Represents a coverage file on disk. The main purpose is to avoid reading the entire file into memory as this
 * dramatically increases the memory footprint of the JVM which might run out of memory because of this.
 */
public class CoverageFile {

	private final File coverageFile;
	private int referenceCounter = 0;

	public CoverageFile(File coverageFile) {
		this.coverageFile = coverageFile;
	}

	/**
	 * Marks the file as being used by an additional uploader. This ensures that the file is not deleted until all users
	 * have signed via {@link #delete()} that they no longer intend to access the file.
	 */
	public CoverageFile acquireReference() {
		referenceCounter++;
		return this;
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

	/** Get the filename of the coverage file. */
	public String getName() {
		return coverageFile.getName();
	}

	/**
	 * Delete the coverage file from disk
	 */
	public void delete() throws IOException {
		referenceCounter--;
		if (referenceCounter <= 0) {
			Files.delete(coverageFile.toPath());
		}
	}

	/**
	 * Create a {@link okhttp3.MultipartBody} form body with the contents of the coverage file.
	 */
	public RequestBody createFormRequestBody() {
		return RequestBody.create(MultipartBody.FORM, new File(coverageFile.getAbsolutePath()));
	}

	/**
	 * Get the {@link java.io.OutputStream} in order to write to the coverage file.
	 *
	 * @throws IOException If the file did not exist yet and could not be created
	 */
	public OutputStream getOutputStream() throws IOException {
		try {
			return new FileOutputStream(coverageFile);
		} catch (IOException e) {
			throw new IOException("Could not create temporary coverage file" + this + ". " +
					"This is used to cache the coverage file on disk before uploading it to its final destination. " +
					"This coverage is lost. Please fix the underlying issue to avoid losing coverage.", e);
		}

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
