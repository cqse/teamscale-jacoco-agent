package com.teamscale.jacoco.agent.upload;

import java.util.Properties;

import com.teamscale.report.jacoco.CoverageFile;

/**
 * Interface for all the uploaders that support an automatic upload retry
 * mechanism.
 */
public interface IUploadRetry {

	/**
	 * Marks coverage files of unsuccessful coverage uploads so that they can be
	 * reuploaded at next agent start.
	 */
	void markFileForUploadRetry(CoverageFile coverageFile);

	/**
	 * Retries previously unsuccessful coverage uploads with the given properties.
	 */
	void reupload(CoverageFile coverageFile, Properties properties);
}
