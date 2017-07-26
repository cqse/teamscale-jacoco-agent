package eu.cqse.teamscale.jacoco.recorder.upload;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;

public class TeamscaleClient {
    private final TeamscaleService service;

    public TeamscaleClient(String baseUrl, String user, String accessToken) {
        service = TeamscaleServiceGenerator
                .createService(TeamscaleService.class, baseUrl, user, accessToken);
    }

    public void upload(byte[] report, String projectName, String timestamp, String partition) throws IOException {
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), report);
        Response<ResponseBody> response = service.uploadExternalReport(projectName, TeamscaleService.ExternalReportFormat.JACOCO, timestamp, true,
                partition, "Manually recorded coverage upload", requestFile)
                .execute();
        if(!response.isSuccessful() || !response.body().string().equals(TeamscaleService.SUCCESS_STRING)) {
            throw new IOException(response.errorBody().string());
        }
    }
}
