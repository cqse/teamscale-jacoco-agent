package com.teamscale.jacoco.agent.upload;

import java.io.File;

/** Uploads coverage reports. */
public interface IUploader {

	/** Uploads the given coverage file. */
	void upload(File coverageFile);

	/** Human-readable description of the uploader. */
	String describe();

}
