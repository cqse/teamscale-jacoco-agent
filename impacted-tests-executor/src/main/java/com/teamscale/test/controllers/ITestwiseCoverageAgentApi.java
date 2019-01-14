package com.teamscale.test.controllers;

import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** {@link Retrofit} API specification for the teamscale agent in testwise coverage mode. */
public interface ITestwiseCoverageAgentApi {

	/** Test start. */
	@POST("test/start/{testUniformPath}")
	Call<ResponseBody> testStarted(@Path("testUniformPath") String testUniformPath);

	/** Test finished. */
	@POST("test/end/{testUniformPath}")
	Call<ResponseBody> testFinished(@Path("testUniformPath") String testUniformPath);

	/**
	 * Generates a {@link Retrofit} instance for the given
	 * service, which uses basic auth to authenticate against the server and which sets the accept header to json.
	 */
	static ITestwiseCoverageAgentApi createService(HttpUrl baseUrl) {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.build();
		return retrofit.create(ITestwiseCoverageAgentApi.class);
	}
}