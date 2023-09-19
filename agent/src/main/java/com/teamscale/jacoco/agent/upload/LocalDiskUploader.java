package com.teamscale.jacoco.agent.upload;

import java.util.Properties;

import com.teamscale.report.jacoco.CoverageFile;

/**
 * Dummy uploader which keeps the coverage file written by the agent on disk,
 * but does not actually perform uploads.
 */
public class LocalDiskUploader implements IUploader {
	@Override
	public void upload(CoverageFile coverageFile) {
		// Don't delete the file here. We want to store the file permanently on disk in
		// case no uploader is configured.
	}

	@Override
	public String describe() {
		return "configured output directory on the local disk";
	}

	@Override
	public void reupload(CoverageFile coverageFile, Properties reuploadProperties) {
		// Intentionally left blank
	}
}
