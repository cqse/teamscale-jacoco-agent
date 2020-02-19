package com.teamscale.jacoco.agent.util;

import com.teamscale.jacoco.agent.store.IUploader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class InMemoryUploader implements IUploader {
	List<File> coverageFiles = new ArrayList<>();

	@Override
	public void upload(File coverageFile) {
		coverageFiles.add(coverageFile);
		coverageFile.delete();
	}

	@Override
	public String describe() {
		return "in memory uploader";
	}

	public List<File> getUploadedFiles() {
		return coverageFiles;
	}
}
