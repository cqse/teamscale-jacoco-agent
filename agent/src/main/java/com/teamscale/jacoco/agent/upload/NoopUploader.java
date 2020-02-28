package com.teamscale.jacoco.agent.upload;

import com.teamscale.report.jacoco.CoverageFile;

/**
 * Dummy uploader that does not provide any functionality. Can be used instead of null to mimic no upload behaviour.
 */
public class NoopUploader implements IUploader {
	@Override
	public void upload(CoverageFile coverageFile) {
		// Don't delete the file here. We want to store the file permanently on disk in case no uploader is configured.
	}

	@Override
	public String describe() {
		return "configured output directory on the local disk without upload";
	}
}
