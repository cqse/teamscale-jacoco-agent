package eu.cqse.test.coverage.recorder.upload;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface TeamscaleService {

    String SUCCESS_STRING = "success";

    enum ExternalReportFormat {
        JACOCO,
        COBERTURA,
        GCOV,
        LCOV,
        CTC,
        XR_BABOON,
        MS_COVERAGE
    }

    @Multipart
    @POST("p/{projectName}/external-report/")
    Call<ResponseBody> uploadExternalReport(@Path("projectName") String projectName,
                                            @Query("format") ExternalReportFormat format,
                                            @Query("t") String timestamp,
                                            @Query("adjusttimestamp") boolean adjustTimestamp,
                                            @Query("partition") String partition,
                                            @Query("message") String message,
                                            @Part("report") RequestBody report
    );
}