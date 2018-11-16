/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.store.upload.http;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

import java.io.IOException;

/** {@link Retrofit} API specification for the {@link HttpUploadStore}. */
public interface IHttpUploadApi {

	/** The upload API call. */
	@Multipart
	@POST("/")
	public Call<ResponseBody> upload(@Part MultipartBody.Part uploadedFile);

	/**
	 * Convenience method to perform an {@link #upload(okhttp3.MultipartBody.Part)}
	 * call for a coverage zip.
	 */
	public default Response<ResponseBody> uploadCoverageZip(byte[] data) throws IOException {
		RequestBody body = RequestBody.create(MediaType.parse("application/zip"), data);
		MultipartBody.Part part = MultipartBody.Part.createFormData("file", "coverage.zip", body);
		return upload(part).execute();
	}

}
