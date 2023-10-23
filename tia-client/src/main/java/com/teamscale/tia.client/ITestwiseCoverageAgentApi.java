package com.teamscale.tia.client;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.TestExecution;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** {@link Retrofit} API specification for the JaCoCo agent in test-wise coverage mode. */
public interface ITestwiseCoverageAgentApi {

	/** Set the partition name as shown in Teamscale. */
	@PUT("partition")
	Call<ResponseBody> setPartition(@Body String partition);

	/** Set the revision as shown in Teamscale. */
	@PUT("revision")
	Call<ResponseBody> setRevision(@Body String partition);

	/** Set the upload commit as shown in Teamscale. */
	@PUT("commit")
	Call<ResponseBody> setCommit(@Body String commit);

	/** Set the commit message with which the upload is shown in Teamscale. */
	@PUT("message")
	Call<ResponseBody> setMessage(@Body String message);

	/** Test start. */
	@POST("test/start/{testUniformPath}")
	Call<ResponseBody> testStarted(@Path(value = "testUniformPath", encoded = true) String testUniformPath);

	/** Test finished. */
	@POST("test/end/{testUniformPath}")
	Call<ResponseBody> testFinished(
			@Path(value = "testUniformPath", encoded = true) String testUniformPath
	);

	/** Test finished. */
	@POST("test/end/{testUniformPath}")
	Call<ResponseBody> testFinished(
			@Path(value = "testUniformPath", encoded = true) String testUniformPath,
			@Body TestExecution testExecution
	);

	/**
	 * Test run started. Returns a single dummy cluster of TIA-selected and -prioritized tests
	 * that Teamscale currently knows about.
	 */
	@POST("testrun/start")
	Call<List<PrioritizableTestCluster>> testRunStarted(
			@Query("include-non-impacted") boolean includeNonImpacted,
			@Query("baseline") Long baseline
	);

	/**
	 * Test run started. Returns the list of TIA-selected and -prioritized test clusters to execute.
	 */
	@POST("testrun/start")
	Call<List<PrioritizableTestCluster>> testRunStarted(
			@Query("include-non-impacted") boolean includeNonImpacted,
			@Query("baseline") Long baseline,
			@Body List<ClusteredTestDetails> availableTests
	);

	/**
	 * Test run finished. Generate test-wise coverage report and upload to Teamscale.
	 *
	 * @param partial Whether the test recording only contains a subset of the available tests.
	 */
	@POST("testrun/end")
	Call<ResponseBody> testRunFinished(@Query("partial") Boolean partial);

	/**
	 * Generates a {@link Retrofit} instance for this service, which uses basic auth to authenticate against the server
	 * and which sets the Accept header to JSON.
	 */
	static ITestwiseCoverageAgentApi createService(HttpUrl baseUrl) {
		OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
		httpClientBuilder.connectTimeout(60, TimeUnit.SECONDS);
		httpClientBuilder.readTimeout(120, TimeUnit.SECONDS);
		httpClientBuilder.writeTimeout(60, TimeUnit.SECONDS);
		Retrofit retrofit = new Retrofit.Builder()
				.client(httpClientBuilder.build()) //
				.baseUrl(baseUrl) //
				.addConverterFactory(MoshiConverterFactory.create()) //
				.build();
		return retrofit.create(ITestwiseCoverageAgentApi.class);
	}
}
