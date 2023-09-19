package com.teamscale.jacoco.agent.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.report.jacoco.CoverageFile;

/**
 * Simulates an upload by storing coverage {@link File} in a list. The
 * "uploaded" Files can then be retrieved with
 * {@link InMemoryUploader#getUploadedFiles()}
 */
public class InMemoryUploader implements IUploader {
	private final List<CoverageFile> coverageFiles = new ArrayList<>();

	@Override
	public void upload(CoverageFile coverageFile) {
		coverageFiles.add(coverageFile);
		try {
			coverageFile.delete();
		} catch (IOException e) {
			// Do nothing as not being able to delete the file is not important for tests
		}
	}

	@Override
	public void reupload(CoverageFile coverageFile, Properties reuploadProperties) {
		// Intentionally left blank.
	}

	@Override
	public String describe() {
		return "in memory uploader";
	}

	public List<CoverageFile> getUploadedFiles() {
		return coverageFiles;
	}
}
