package eu.cqse.teamscale.test.upload;

import eu.cqse.teamscale.config.Server;
import eu.cqse.teamscale.test.TestCase;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static eu.cqse.teamscale.test.upload.TeamscaleService.SUCCESS_STRING;

@SuppressWarnings({"WeakerAccess", "unused"})
public class TeamscaleClient {
    private final TeamscaleService service;
    private final String projectId;

    public TeamscaleClient(Server server) {
        this(server.url, server.userName, server.userAccessToken, server.project);
    }

    public TeamscaleClient(String baseUrl, String user, String accessToken, String projectId) {
        this.projectId = projectId;
        service = TeamscaleServiceGenerator
                .createService(TeamscaleService.class, baseUrl, user, accessToken);
    }

    public void uploadReport(TeamscaleService.EReportFormat reportFormat, File report, CommitDescriptor timestamp, String partition, String message) throws IOException {
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), report);
        Response<ResponseBody> response = service.uploadExternalReport(projectId, reportFormat, timestamp, true,
                partition, message, requestFile)
                .execute();
        if (!response.isSuccessful() || !response.body().string().equals(SUCCESS_STRING)) {
            throw new IOException(response.errorBody().string());
        }
    }

    public Response<List<TestCase>> getSuggestedTests(CommitDescriptor baseline, CommitDescriptor end) throws IOException {
        return service.getSuggestedTests(projectId, "", baseline, end, true).execute();
    }

    public Response<List<TestCase>> getSuggestedTests(CommitDescriptor baseline, CommitDescriptor end, boolean cutoff, String sourceFileName, String method) throws IOException {
        return service.getSuggestedTests(projectId, "", baseline, end, cutoff, "/"+sourceFileName + "!" + method).execute();
    }
}
