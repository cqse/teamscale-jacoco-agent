package com.teamscale.jacoco.agent.upload;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import com.teamscale.jacoco.agent.Agent;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.jacoco.CoverageFile;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/** Base class for tests regarding Teamscale/Artifactory uploads. */
public class UploadTestBase {

	/** The url of the mockserver */
	protected String serverUrl = "/someUrl/";

	/** The mock server to run requests against. */
	protected MockWebServer mockWebServer;
	/** Uploader that will be set in child classes */
	public IUploader uploader;

	/** The coverage file to test the upload with */
	public CoverageFile coverageFile;

	/** Starts the mock server. */
	@BeforeEach
	public void setup(@TempDir File tmpDir) throws Exception {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		File tmpFile = new File(tmpDir.getPath() + File.separator + "tmpfile");
		tmpFile.createNewFile();
		coverageFile = new CoverageFile(tmpFile);
	}

	/**
	 * After unsuccessfully uploading coverage, this method starts the agent which
	 * triggers the automatic upload retry of the remaining coverage.
	 */
	protected void startAgentAfterUploadFailure(AgentOptions options) throws UploaderException {
		options.setParentOutputDirectory(Paths.get(coverageFile.toString()).getParent());
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));
		// Agent is started to check automatic upload retry.
		new Agent(options, null);
	}

	/** Shuts down the mock server. */
	@AfterEach
	public void cleanup() throws Exception {
		mockWebServer.shutdown();
	}
}
