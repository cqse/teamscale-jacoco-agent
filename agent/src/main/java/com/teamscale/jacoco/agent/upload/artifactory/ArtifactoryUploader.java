package com.teamscale.jacoco.agent.upload.artifactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.conqat.lib.commons.filesystem.FileSystemUtils;

import com.google.common.base.Strings;
import com.teamscale.client.EReportFormat;
import com.teamscale.client.HttpUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.upload.HttpZipUploaderBase;
import com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties;
import com.teamscale.report.jacoco.CoverageFile;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Uploads XMLs to Artifactory.
 */
public class ArtifactoryUploader extends HttpZipUploaderBase<IArtifactoryUploadApi> {

	/**
	 * The suffix to add to the metafile that will be created to automatically retry
	 * unsuccessful coverage uploads.
	 */
	public static final String ARTIFACTORY_RETRY_UPLOAD_FILE_SUFFIX = "_artifactory-retry.properties";

	/**
	 * Header that can be used as alternative to basic authentication to
	 * authenticate requests against artifactory. For details check
	 * https://www.jfrog.com/confluence/display/JFROG/Artifactory+REST+API
	 */
	public static final String ARTIFACTORY_API_HEADER = "X-JFrog-Art-Api";
	private final ArtifactoryConfig artifactoryConfig;
	private final String coverageFormat;
	private String uploadPath;

	/** Constructor. */
	public ArtifactoryUploader(ArtifactoryConfig config, List<Path> additionalMetaDataFiles,
			EReportFormat reportFormat) {
		super(config.url, additionalMetaDataFiles, IArtifactoryUploadApi.class);
		this.artifactoryConfig = config;
		this.coverageFormat = reportFormat.name().toLowerCase();
	}

	@Override
	public void markFileForUploadRetry(CoverageFile coverageFile) {
		File uploadMetadataFile = new File(FileSystemUtils.replaceFilePathFilenameWith(
				com.teamscale.client.FileSystemUtils.normalizeSeparators(coverageFile.toString()),
				coverageFile.getName() + ARTIFACTORY_RETRY_UPLOAD_FILE_SUFFIX));
		Properties properties = createArtifactoryProperties();
		try {
			FileWriter writer = new FileWriter(uploadMetadataFile);
			properties.store(writer, null);
			writer.close();
		} catch (IOException e) {
			logger.warn(
					"Failed to create metadata file for automatic upload retry of {}. Please manually retry the coverage upload to Azure.",
					coverageFile);
			uploadMetadataFile.delete();
		}
	}

	/** Creates properties from the artifactory configs. */
	private Properties createArtifactoryProperties() {
		Properties properties = new Properties();
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_URL_OPTION, artifactoryConfig.url.toString());
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_USER_OPTION, Strings.nullToEmpty(artifactoryConfig.user));
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_PASSWORD_OPTION,
				Strings.nullToEmpty(artifactoryConfig.password));
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_LEGACY_PATH_OPTION,
				String.valueOf(artifactoryConfig.legacyPath));
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_ZIP_PATH_OPTION,
				Strings.nullToEmpty(artifactoryConfig.zipPath));
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_PATH_SUFFIX,
				Strings.nullToEmpty(artifactoryConfig.pathSuffix));
		properties.setProperty(ETeamscaleServerProperties.REVISION.name(), artifactoryConfig.commitInfo.revision);
		properties.setProperty(ETeamscaleServerProperties.COMMIT.name(),
				artifactoryConfig.commitInfo.commit.toString());
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_GIT_PROPERTIES_COMMIT_DATE_FORMAT_OPTION,
				artifactoryConfig.dateTimeFormatterPattern);
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION, artifactoryConfig.apiKey);
		properties.setProperty(ArtifactoryConfig.ARTIFACTORY_PARTITION,
				Strings.nullToEmpty(artifactoryConfig.partition));
		setAdditionalMetaDataPathProperties(properties);
		properties.setProperty("REPORT_FORMAT", this.coverageFormat);
		return properties;
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
		if (artifactoryConfig.legacyPath) {
			this.uploadPath = String.join("/", artifactoryConfig.commitInfo.commit.branchName,
					artifactoryConfig.commitInfo.commit.timestamp + "-" + artifactoryConfig.commitInfo.revision,
					coverageFile.getNameWithoutExtension() + ".zip");
		} else if (artifactoryConfig.pathSuffix == null) {
			this.uploadPath = String.join("/", "uploads", artifactoryConfig.commitInfo.commit.branchName,
					artifactoryConfig.commitInfo.commit.timestamp + "-" + artifactoryConfig.commitInfo.revision,
					artifactoryConfig.partition, coverageFormat, coverageFile.getNameWithoutExtension() + ".zip");
		} else {
			this.uploadPath = String.join("/", "uploads", artifactoryConfig.commitInfo.commit.branchName,
					artifactoryConfig.commitInfo.commit.timestamp + "-" + artifactoryConfig.commitInfo.revision,
					artifactoryConfig.partition, coverageFormat, artifactoryConfig.pathSuffix,
					coverageFile.getNameWithoutExtension() + ".zip");
		}
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
