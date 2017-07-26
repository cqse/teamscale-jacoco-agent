package eu.cqse.teamscale.test.upload;

import eu.cqse.teamscale.test.TestCase;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface TeamscaleService {

    String SUCCESS_STRING = "success";

    enum EReportFormat {
        JACOCO,
        COBERTURA,
        GCOV,
        LCOV,
        CTC,
        XR_BABOON,
        MS_COVERAGE,
        DOT_COVER,
        ROSLYN,
        JUNIT,
        SIMPLE,
        CPPCHECK,
        PCLINT,
        CLANG
    }

    @Multipart
    @POST("p/{projectName}/external-report/")
    Call<ResponseBody> uploadExternalReport(@Path("projectName") String projectName,
                                            @Query("format") EReportFormat format,
                                            @Query("t") CommitDescriptor commit,
                                            @Query("adjusttimestamp") boolean adjustTimestamp,
                                            @Query("partition") String partition,
                                            @Query("message") String message,
                                            @Part("report") RequestBody report
    );

    @GET("p/{projectName}/suggested-tests/{uniformPath}")
    Call<List<TestCase>> getSuggestedTests(@Path("projectName") String projectName,
                                           @Path("uniformPath") String uniformPath,
                                           @Query("baseline") CommitDescriptor baseline,
                                           @Query("end") CommitDescriptor end,
                                           @Query("cutoff") boolean cutoff);

    @GET("p/{projectName}/suggested-tests/{uniformPath}")
    Call<List<TestCase>> getSuggestedTests(@Path("projectName") String projectName,
                                           @Path("uniformPath") String uniformPath,
                                           @Query("baseline") CommitDescriptor baseline,
                                           @Query("end") CommitDescriptor end,
                                           @Query("cutoff") boolean cutoff,
                                           @Query("modified") String pathMethodId);

}