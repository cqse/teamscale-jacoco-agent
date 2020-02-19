package com.teamscale.jacoco.agent.store.upload;

import com.teamscale.client.HttpUtils;
import com.teamscale.jacoco.agent.store.IUploader;
import com.teamscale.jacoco.agent.store.UploaderException;
import com.teamscale.jacoco.agent.util.Benchmark;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Base class for uploading the coverage zip to a provided url */
public abstract class UploadStoreBase<T> implements IUploader {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** The URL to upload to. */
	protected final HttpUrl uploadUrl;

	/** Additional files to include in the uploaded zip. */
	protected final List<Path> additionalMetaDataFiles;

	/** The API which performs the upload */
	protected final T api;

	/** Constructor. */
	public UploadStoreBase(HttpUrl uploadUrl, List<Path> additionalMetaDataFiles) {
		this.uploadUrl = uploadUrl;
		this.additionalMetaDataFiles = additionalMetaDataFiles;

		Retrofit retrofit = HttpUtils.createRetrofit(retrofitBuilder -> retrofitBuilder.baseUrl(uploadUrl));
		api = getApi(retrofit);
	}

	/** Returns the API for creating request to the http store */
	protected abstract T getApi(Retrofit retrofit);

	/** Uploads the coverage zip to the server */
	protected abstract Response<ResponseBody> uploadCoverageZip(
			File coverageFile) throws IOException, UploaderException;

	@Override
	public void upload(File coverageFile) {
		try (Benchmark ignored = new Benchmark("Uploading report via HTTP")) {
			if (!tryUpload(coverageFile)) {
				logger.warn("Failed to upload coverage. Won't delete local file");
			} else {
				coverageFile.delete();
			}
		}
	}

	/** Performs the upload and returns <code>true</code> if successful. */
	protected boolean tryUpload(File coverageFile) {
		logger.debug("Uploading coverage to {}", uploadUrl);

		File zipFile;
		try {
			zipFile = createZipFile(coverageFile);
		} catch (IOException e) {
			logger.error("Failed to compile coverage zip file for upload to {}", uploadUrl, e);
			return false;
		}

		try {
			Response<ResponseBody> response = uploadCoverageZip(zipFile);
			if (response.isSuccessful()) {
				zipFile.delete();
				return true;
			}

			String errorBody = "";
			if (response.errorBody() != null) {
				errorBody = response.errorBody().string();
			}

			logger.error("Failed to upload coverage to {}. Request failed with error code {}. Error:\n{}",
					uploadUrl, response.code(), errorBody);
			return false;
		} catch (IOException e) {
			logger.error("Failed to upload coverage to {}. Probably a network problem", uploadUrl, e);
			return false;
		} catch (UploaderException e) {
			logger.error("Failed to upload coverage to {}. The configuration is probably incorrect", uploadUrl, e);
			return false;
		}
	}

	/**
	 * Creates the zip file to upload which includes the given coverage XML and all {@link #additionalMetaDataFiles}.
	 */
	private File createZipFile(File coverageFile) throws IOException {
		File zipFile = createZipFileObject(coverageFile);
		try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile)) {
			try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
				fillZipFile(zipOutputStream, coverageFile);
			}
			return zipFile;
		}
	}

	private File createZipFileObject(File coverageFile) {
		Path parentFolder = coverageFile.getParentFile().toPath();
		String filename = coverageFile.getName().substring(0, coverageFile.getName().length() - 4) + ".zip";
		return new File(parentFolder.resolve(filename).toString());
	}

	/**
	 * Fills the upload zip file with the given coverage XML and all {@link #additionalMetaDataFiles}.
	 */
	private void fillZipFile(ZipOutputStream zipOutputStream, File coverageFile)
			throws IOException {
		zipOutputStream.putNextEntry(new ZipEntry("coverage.xml"));
		zipOutputStream.write(FileSystemUtils.readFileBinary(coverageFile));

		for (Path additionalFile : additionalMetaDataFiles) {
			zipOutputStream.putNextEntry(new ZipEntry(additionalFile.getFileName().toString()));
			zipOutputStream.write(FileSystemUtils.readFileBinary(additionalFile.toFile()));
		}
	}
}
