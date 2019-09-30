package com.teamscale.jacoco.agent.store;

import java.io.File;

public class NoopUploader implements IUploader {
	@Override
	public void upload(File coverageFile) {

	}

	@Override
	public String describe() {
		return "No upload configured";
	}
}
