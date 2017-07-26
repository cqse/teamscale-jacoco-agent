package eu.cqse.test.coverage.recorder.upload;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class TeamscaleServiceGenerator {

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    public static <S> S createService(Class<S> serviceClass, String baseUrl, String username, String password) {
        if (username != null && password != null) {
            String credentials = username + ":" + password;
            final String basic = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

            httpClient.addInterceptor(chain -> {
                Request original = chain.request();

                Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", basic)
                        .header("Accept", "application/json")
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            });
        }
        httpClient.connectTimeout(5, TimeUnit.MINUTES);
        httpClient.readTimeout(5, TimeUnit.MINUTES);
        httpClient.writeTimeout(5, TimeUnit.MINUTES);

        OkHttpClient client = httpClient.build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .build();
        return retrofit.create(serviceClass);
    }
}
