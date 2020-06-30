package com.teamscale.jacoco.agent.upload.artifactory;

import com.teamscale.client.HttpUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.upload.HttpZipUploaderBase;
import com.teamscale.report.jacoco.CoverageFile;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Uploads XMLs to Artifactory.
 */
public class ArtifactoryUploader extends HttpZipUploaderBase<IArtifactoryUploadApi> {
	private final ArtifactoryConfig artifactoryConfig;
	private String uploadPath;

	/** Constructor. */
	public ArtifactoryUploader(ArtifactoryConfig config, List<Path> additionalMetaDataFiles) {
		super(config.url, additionalMetaDataFiles, IArtifactoryUploadApi.class);
		this.artifactoryConfig = config;
	}

	@Override
	protected void configureOkHttp(OkHttpClient.Builder builder) {
		super.configureOkHttp(builder);
		builder
				.addInterceptor(HttpUtils.getBasicAuthInterceptor(artifactoryConfig.user, artifactoryConfig.password));
	}

	@Override
	public void upload(CoverageFile coverageFile) {
		this.uploadPath = String.join("/", artifactoryConfig.commitInfo.commit.branchName,
				artifactoryConfig.commitInfo.commit.timestamp + "-" + artifactoryConfig.commitInfo.revision,
				coverageFile.getNameWithoutExtension() + ".zip");
		super.upload(coverageFile);
	}

	@Override
	protected Response<ResponseBody> uploadCoverageZip(File zipFile) throws IOException {
		return getApi().uploadCoverageZip(uploadPath, zipFile);
	}

	@Override
	protected String getZipEntryCoverageFileName(CoverageFile coverageFile) {
		String path = coverageFile.getName();
		if (!StringUtils.isEmpty(artifactoryConfig.zipPath)) {
			path = artifactoryConfig.zipPath + "/" + path;
		}

		return path;
	}

	/** {@inheritDoc} */
	@Override
	public String describe() {
		return "Uploading to " + uploadUrl;
	}
}
