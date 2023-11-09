package com.teamscale.profiler.installer;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/** Utilities for interacting with a Teamscale instance. */
public class TeamscaleUtils {

	private static boolean validateSsl = true;

	/** Disables SSL validation for all Teamscale connections via this class. */
	public static void disableSslValidation() {
		validateSsl = false;
	}

	/** Ensures that we can connect to Teamscale with the given credentials. */
	public static void checkTeamscaleConnection(TeamscaleCredentials credentials) throws FatalInstallerError {
		OkHttpClient client = OkHttpUtils.createClient(validateSsl, 30);

		// we use the project list as a test to see if the Teamscale credentials are valid.
		// any other API that requires a logged-in user would be fine as well.
		HttpUrl url = credentials.url.resolve("api/v8.8/projects");
		if (url == null) {
			// this should never happen but the API forces us to handle this
			throw new IllegalStateException("Cannot resolve API endpoint against URL " + credentials.url);
		}

		Request request = new Request.Builder()
				.url(url)
				.header("Authorization", Credentials.basic(credentials.username, credentials.accessKey))
				.build();

		try (Response response = client.newCall(request).execute()) {
			handleErrors(response, credentials);
		} catch (SSLException e) {
			throw new FatalInstallerError("Failed to connect via HTTPS to " + credentials.url
					+ "\nPlease ensure that your Teamscale instance is reachable under " + credentials.url
					+ " and that it is configured for HTTPS, not HTTP. E.g. open that URL in your"
					+ " browser and verify that you can connect successfully."
					+ "\n\nIf you want to accept self-signed or broken certificates without an error"
					+ " you can use --insecure.",
					e);
		} catch (UnknownHostException e) {
			throw new FatalInstallerError("The host " + url + " could not be resolved."
					+ " Please ensure you have no typo and that this host is reachable from this server.",
					e);
		} catch (ConnectException e) {
			throw new FatalInstallerError("The URL " + url + " refused a connection."
					+ " Please ensure that you have no typo and that this endpoint is reachable and not blocked by firewalls.",
					e);
		} catch (
				SocketTimeoutException e) {
			throw new FatalInstallerError("Request timeout reached."
					+ " Please ensure that you have no typo and that this endpoint is reachable and not blocked by firewalls.",
					e);
		} catch (IOException e) {
			throw new FatalInstallerError(
					"Teamscale is not reachable from this machine. Please check firewall settings.", e);
		}
	}

	private static void handleErrors(Response response, TeamscaleCredentials credentials) throws FatalInstallerError {
		if (response.isRedirect()) {
			String location = response.header("Location");
			if (location == null) {
				location = "<server did not provide a location header>";
			}
			throw new FatalInstallerError("You provided an incorrect URL."
					+ " The server responded with a redirect to " + "'" + location + "'."
					+ " This may e.g. happen if you used HTTP instead of HTTPS."
					+ " Please use the correct URL for Teamscale instead.");
		}

		if (response.code() == 401) {
			String editUserUrl = TeamscaleUtils.getEditUserUrl(credentials.url, credentials.username);
			throw new FatalInstallerError("You provided incorrect credentials."
					+ " Either the user '" + credentials.username + "' does not exist in Teamscale"
					+ " or the access key you provided is incorrect."
					+ " Please check both the username and access key in Teamscale under Admin > Users: "
					+ editUserUrl + "\nPlease use the user's access key, not their password.");
		}

		if (!response.isSuccessful()) {
			throw new FatalInstallerError(
					"Unexpected response from Teamscale, HTTP status " + response.code() + " " + response.message());
		}
	}


	/** Returns a URL to the edit page of the given user. */
	private static String getEditUserUrl(HttpUrl teamscaleBaseUrl, String username) {
		return fixFragment(teamscaleBaseUrl.newBuilder().addPathSegment("admin.html#users")
				.addQueryParameter("action", "edit").addQueryParameter("username", username));
	}

	/**
	 * Teamscale requires that the fragment be present before the query parameters. OkHttp always encodes the fragment
	 * after the query parameters. So we have to encode the fragment in the path, which unfortunately escapes the "#"
	 * separator. This function undoes this unwanted encoding.
	 */
	private static String fixFragment(HttpUrl.Builder url) {
		return url.toString().replaceFirst("%23", "#");
	}
}
