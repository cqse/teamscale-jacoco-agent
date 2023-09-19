package com.teamscale.jacoco.agent.upload;

import java.util.Properties;

import com.teamscale.report.jacoco.CoverageFile;

/** Uploads coverage reports. */
public interface IUploader {

	/**
	 * Uploads the given coverage file. If the upload was successful, the coverage
	 * file on disk will be deleted. Otherwise the file is left on disk and a
	 * warning is logged.
	 */
	void upload(CoverageFile coverageFile);

	/** Human-readable description of the uploader. */
	String describe();

	/**
	 * Retries previously unsuccessful coverage uploads with the given properties.
	 */
	void reupload(CoverageFile coverageFile, Properties reuploadProperties);

}
