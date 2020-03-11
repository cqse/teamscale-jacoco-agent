package com.teamscale.jacoco.agent.upload.http;

import com.teamscale.jacoco.agent.upload.HttpZipUploaderBase;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Uploads XMLs and metadata via HTTP multi-part form data requests.
 */
public class HttpUploader extends HttpZipUploaderBase<IHttpUploadApi> {
	/** Constructor. */
	public HttpUploader(HttpUrl uploadUrl, List<Path> additionalMetaDataFiles) {
		super(uploadUrl, additionalMetaDataFiles);
	}

	@Override
	protected IHttpUploadApi getApi(Retrofit retrofit) {
		return retrofit.create(IHttpUploadApi.class);
	}

	@Override
	protected Response<ResponseBody> uploadCoverageZip(File zipFile) throws IOException {
		return api.uploadCoverageZip(zipFile);
	}

	/** {@inheritDoc} */
	@Override
	public String describe() {
		return "Uploading to " + uploadUrl;
	}
}
