import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Properties;
import java.util.stream.Stream;

public class Installer {

	private static final Path SOURCE_DIRECTORY = Paths.get(".");
	private static final Path INSTALL_DIRECTORY = Paths.get("/opt/teamscale-profiler/java");
	private static final Path COVERAGE_DIRECTORY = INSTALL_DIRECTORY.resolve("coverage");
	private static final Path LOG_DIRECTORY = INSTALL_DIRECTORY.resolve("logs");
	private static final Path TEAMSCALE_PROPERTIES_PATH = INSTALL_DIRECTORY.resolve("teamscale.properties");

	public static void main(String[] args) {
		new Installer().run(args);
	}

	private static class CommandlineUsageError extends FatalInstallerError {

		public CommandlineUsageError(String cause) {
			super(cause + "\n\nUSAGE: install-profiler [TEAMSCALE URL] [TEAMSCALE USER] [ACCESS KEY]");
		}

	}

	public void run(String[] args) {
		try {
			TeamscaleCredentials credentials = parseCredentials(args);
			checkTeamscaleConnection(credentials);
			createAgentDirectory();
			copyAgentFiles();
			writeTeamscaleProperties(credentials);
			makeCoverageAndLogDirectoriesWorldWritable();
			makeAllProfilerFilesWorldReadable();
			enableSystemWide();
			printSuccessMessage();
			System.exit(0);
		} catch (FatalInstallerError e) {
			System.err.println("\n\nInstallation failed: " + e.getMessage());
			if (e.getCause() != null) {
				e.printStackTrace(System.err);
			}
			System.exit(1);
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.err.println("\n\nInstallation failed due to an internal error." +
					" This is likely a bug, please report the entire console output to support@teamscale.com");
			System.exit(2);
		}
	}

	private void printSuccessMessage() {
		System.out.println("Installation successful. Profiler installed to ???"); // TODO (FS)
	}

	private void enableSystemWide() {
	}

	private void makeAllProfilerFilesWorldReadable() throws FatalInstallerError {
		try (Stream<Path> fileStream = Files.walk(INSTALL_DIRECTORY)) {
			for (Iterator<Path> it = fileStream.iterator(); it.hasNext(); ) {
				Path path = it.next();
				InstallUtils.makeReadable(path);
			}
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to list all files in " + INSTALL_DIRECTORY + ".", e);
		}
	}

	private void makeCoverageAndLogDirectoriesWorldWritable() throws FatalInstallerError {
		InstallUtils.createDirectory(COVERAGE_DIRECTORY);
		InstallUtils.makeWritable(COVERAGE_DIRECTORY);

		InstallUtils.createDirectory(LOG_DIRECTORY);
		InstallUtils.makeWritable(LOG_DIRECTORY);

	}

	private void writeTeamscaleProperties(TeamscaleCredentials credentials) throws FatalInstallerError {
		Properties properties = new Properties();
		properties.setProperty("url", credentials.url.toString());
		properties.setProperty("username", credentials.username);
		properties.setProperty("accesskey", credentials.accessKey);

		try (OutputStream out = Files.newOutputStream(TEAMSCALE_PROPERTIES_PATH, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE)) {
			properties.store(out, null);
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to write " + TEAMSCALE_PROPERTIES_PATH + ".", e);
		}
	}

	private void copyAgentFiles() throws FatalInstallerError {
		try {
			FileSystemUtils.copyFiles(SOURCE_DIRECTORY.toFile(), INSTALL_DIRECTORY.toFile(), null);
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to copy some files to " + INSTALL_DIRECTORY + "."
					+ " Please manually clean up " + INSTALL_DIRECTORY, e);
		}
	}

	private void createAgentDirectory() throws FatalInstallerError {
		if (Files.exists(INSTALL_DIRECTORY)) {
			throw new FatalInstallerError("Cannot install to " + INSTALL_DIRECTORY + ": Path already exists");
		}

		InstallUtils.createDirectory(INSTALL_DIRECTORY);
	}

	private void checkTeamscaleConnection(TeamscaleCredentials credentials) throws FatalInstallerError {
		OkHttpClient client = new OkHttpClient();

		// we use the project list as a test to see if the Teamscale credentials are valid.
		// any other API that requires a logged-in user would be fine as well.
		HttpUrl url = credentials.url.resolve("api/v8.8/projects");
		if (url == null) {
			// this should never happen but the API forces us to handle this
			throw new RuntimeException("Cannot resolve API endpoint against URL " + credentials.url);
		}

		Request request = new Request.Builder()
				.url(url)
				.header("Authorization", Credentials.basic(credentials.username, credentials.accessKey))
				.build();

		try (Response response = client.newCall(request).execute()) {
			handleErrors(response, credentials);
		} catch (SSLHandshakeException e) {
			throw new FatalInstallerError("Failed to connect via HTTPS to " + credentials.url
					+ "\nPlease ensure that your Teamscale instance is reachable under " + credentials.url
					+ " and that it is configured for HTTPS, not HTTP. E.g. open that URL in your"
					+ " browser and verify that you can connect successfully.",
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
			String editUserUrl = getEditUserUrl(credentials.url, credentials.username);
			throw new FatalInstallerError("You provided incorrect credentials."
					+ " Either the user '" + credentials.username + "' does not exist in Teamscale"
					+ " or the access key you provided is incorrect."
					+ " Please check both the username and access key in Teamscale under Admin > Users: "
					+ editUserUrl + "\nPlease use the user's access key, not their password.");
		}

		if (!response.isSuccessful()) {
			throw new FatalInstallerError("Unexpected response from Teamscale");
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

	private TeamscaleCredentials parseCredentials(String[] args) throws FatalInstallerError {
		if (args.length < 3) {
			throw new CommandlineUsageError("You must provide 3 command line arguments");
		}

		String urlArgument = args[0];
		HttpUrl url = HttpUrl.parse(urlArgument);
		if (url == null) {
			throw new CommandlineUsageError("This is not a valid URL: " + urlArgument);
		}

		return new TeamscaleCredentials(url, args[1], args[2]);
	}

}
