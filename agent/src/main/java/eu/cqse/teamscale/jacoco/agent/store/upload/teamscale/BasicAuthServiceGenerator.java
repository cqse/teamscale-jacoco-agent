package eu.cqse.teamscale.jacoco.agent.store.upload.teamscale;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

import java.util.Base64;

public class BasicAuthServiceGenerator {

    /**
     * Generates a {@link Retrofit} instance for the given
     * service, which uses basic auth to authenticate against the server.
     */
    public static <S> S createService(Class<S> serviceClass, HttpUrl baseUrl, String username, String password) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(getBasicAuthInterceptor(username, password))
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .build()
                .create(serviceClass);
    }

    /**
     * Returns an interceptor, which adds a basic auth header to a request.
     */
    private static Interceptor getBasicAuthInterceptor(String username, String password) {
        String credentials = username + ":" + password;
        String basic = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        return chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("Authorization", basic).build();
            return chain.proceed(request);
        };
    }
}
