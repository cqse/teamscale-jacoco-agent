package eu.cqse.teamscale.test.upload;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TeamscaleServiceGenerator {

    public static <S> S createService(Class<S> serviceClass, String baseUrl, String username, String password) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.connectTimeout(5, TimeUnit.MINUTES);
        httpClient.readTimeout(5, TimeUnit.MINUTES);
        httpClient.writeTimeout(5, TimeUnit.MINUTES);

        if (username != null && password != null) {
            addAuthenticationToken(httpClient, username, password);
        }

        // httpClient.addInterceptor(new GzipRequestInterceptor());

        OkHttpClient client = httpClient.build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(serviceClass);
    }

    private static void addAuthenticationToken(OkHttpClient.Builder httpClient, String username, String password) {
        String credentials = username + ":" + password;
        final String basic = "Basic " + DatatypeConverter.printBase64Binary(credentials.getBytes());

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", basic)
                        .header("Accept", "application/json")
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });
    }
}
