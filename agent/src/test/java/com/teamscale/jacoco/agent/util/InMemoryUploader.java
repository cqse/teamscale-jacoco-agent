package com.teamscale.jacoco.agent.util;

import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.report.jacoco.CoverageFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates an upload by storing coverage {@link File} in a list. The "uploaded" Files can then be retrieved with
 * {@link InMemoryUploader#getUploadedFiles()}
 */
public class InMemoryUploader implements IUploader {
	private final List<CoverageFile> coverageFiles = new ArrayList<>();

	@Override
	public void upload(CoverageFile coverageFile) {
		coverageFiles.add(coverageFile);
		coverageFile.delete();
	}

	@Override
	public String describe() {
		return "in memory uploader";
	}

	public List<CoverageFile> getUploadedFiles() {
		return coverageFiles;
	}
}
