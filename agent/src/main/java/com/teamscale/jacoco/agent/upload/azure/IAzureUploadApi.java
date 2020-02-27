package com.teamscale.jacoco.agent.upload.azure;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.HEAD;
import retrofit2.http.HeaderMap;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

/** {@link Retrofit} API specification for the {@link AzureFileStorageUploader}. */
public interface IAzureUploadApi {

	/** PUT call to the azure file storage without any data in the body */
	@PUT("{path}")
	public Call<ResponseBody> put(
			@Path(value = "path", encoded = true) String path,
			@HeaderMap Map<String, String> headers,
			@QueryMap Map<String, String> query
	);

	/** PUT call to the azure file storage with data in the body */
	@PUT("{path}")
	public Call<ResponseBody> putData(
			@Path(value = "path", encoded = true) String path,
			@HeaderMap Map<String, String> headers,
			@QueryMap Map<String, String> query,
			@Body RequestBody content
	);

	/** HEAD call to the azure file storage */
	@HEAD("{path}")
	public Call<Void> head(
			@Path(value = "path", encoded = true) String path,
			@HeaderMap Map<String, String> headers,
			@QueryMap Map<String, String> query
	);
}
