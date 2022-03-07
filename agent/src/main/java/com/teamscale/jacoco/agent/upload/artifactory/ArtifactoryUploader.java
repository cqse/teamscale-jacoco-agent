package com.teamscale.jacoco.agent.upload.artifactory;

import com.teamscale.client.HttpUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.upload.HttpZipUploaderBase;
import com.teamscale.report.jacoco.CoverageFile;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
	/**
	 * Header that can be used as alternative to basic authentication to authenticate requests against artifactory. For
	 * details check https://www.jfrog.com/confluence/display/JFROG/Artifactory+REST+API
	 */
	public static final String ARTIFACTORY_API_HEADER = "X-JFrog-Art-Api";
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
		if (artifactoryConfig.apiKey != null) {
			builder.addInterceptor(getArtifactoryApiHeaderInterceptor());
		} else {
			builder.addInterceptor(
					HttpUtils.getBasicAuthInterceptor(artifactoryConfig.user, artifactoryConfig.password));
		}
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

	private Interceptor getArtifactoryApiHeaderInterceptor() {
		return chain -> {
			Request newRequest = chain.request().newBuilder().header(ARTIFACTORY_API_HEADER, artifactoryConfig.apiKey)
					.build();
			return chain.proceed(newRequest);
		};
	}
}
