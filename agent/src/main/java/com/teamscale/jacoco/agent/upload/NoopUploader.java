package com.teamscale.jacoco.agent.upload;

import java.io.File;

/**
 * Dummy uploader that does not provide any functionality. Can be used instead of null to mimic no upload behaviour.
 */
public class NoopUploader implements IUploader {
	@Override
	public void upload(File coverageFile) {

	}

	@Override
	public String describe() {
		return "No upload configured";
	}
}
