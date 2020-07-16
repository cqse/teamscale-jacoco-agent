/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.upload.artifactory;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import java.io.File;
import java.io.IOException;

/** {@link Retrofit} API specification for the {@link ArtifactoryUploader}. */
public interface IArtifactoryUploadApi {

	/** The upload API call. */
	@PUT("{path}")
	Call<ResponseBody> upload(@Path("path") String path, @Body RequestBody uploadedFile);

	/**
	 * Convenience method to perform an upload for a coverage zip.
	 */
	default Response<ResponseBody> uploadCoverageZip(String path, File data) throws IOException {
		RequestBody body = RequestBody.create(MediaType.parse("application/zip"), data);
		return upload(path, body).execute();
	}

}
