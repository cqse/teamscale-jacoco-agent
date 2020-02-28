package com.teamscale.jacoco.agent.upload;

import com.teamscale.report.jacoco.CoverageFile;

/** Uploads coverage reports. */
public interface IUploader {

	/** Uploads the given coverage file. */
	void upload(CoverageFile coverageFile);

	/** Human-readable description of the uploader. */
	String describe();

}
