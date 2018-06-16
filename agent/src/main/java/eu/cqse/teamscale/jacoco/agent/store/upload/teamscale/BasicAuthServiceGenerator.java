package eu.cqse.teamscale.jacoco.agent.store.upload.teamscale;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

import javax.xml.bind.DatatypeConverter;

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
        final String basic = "Basic " + DatatypeConverter.printBase64Binary(credentials.getBytes());

        return chain -> {
            Request original = chain.request();

            Request.Builder requestBuilder = original.newBuilder()
                    .header("Authorization", basic)
                    .header("Accept", "application/json")
                    .method(original.method(), original.body());

            Request request = requestBuilder.build();
            return chain.proceed(request);
        };
    }
}
