package com.teamscale.tia;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.TestExecution;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

/** {@link Retrofit} API specification for the JaCoCo agent in test-wise coverage mode. */
public interface ITestwiseCoverageAgentApi {

	/** Test start. */
	@POST("test/start/{testUniformPath}")
	Call<ResponseBody> testStarted(@Path("testUniformPath") String testUniformPath);

	/** Test finished. */
	@POST("test/end/{testUniformPath}")
	Call<ResponseBody> testFinished(
			@Path("testUniformPath") String testUniformPath,
			@Body TestExecution testExecution
	);

	/** Test run started. Returns the list of TIA-selected and -prioritized test cases to execute. */
	@POST("testrun/start")
	Call<List<PrioritizableTestCluster>> testRunStarted(
			@Query("includeNonImpacted") boolean includeNonImpacted,
			@Query("baseline") Long baseline,
			@Body List<ClusteredTestDetails> availableTests
	);

	/** Test run finished. Generate test-wise coverage report and upload to Teamscale. */
	@POST("testrun/end")
	Call<ResponseBody> testRunFinished();

	/**
	 * Generates a {@link Retrofit} instance for this service, which uses basic auth to authenticate against the server
	 * and which sets the Accept header to JSON.
	 */
	static ITestwiseCoverageAgentApi createService(HttpUrl baseUrl) {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.addConverterFactory(MoshiConverterFactory.create())
				.build();
		return retrofit.create(ITestwiseCoverageAgentApi.class);
	}
}