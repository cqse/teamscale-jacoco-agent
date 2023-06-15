public class Installer {

	public static void main(String[] args) {
		new Installer().run(args);
	}

	public void run(String[] args) {
		try {
			TeamscaleCredentials credentials = parseCredentials(args);
			createAgentDirectory();
			copyAgentFiles();
			writeTeamscaleProperties(credentials);
			setPermissions();
			enableSystemWide();
			printSuccessMessage();
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

	private void setPermissions() {
	}

	private void writeTeamscaleProperties(TeamscaleCredentials credentials) {
	}

	private void copyAgentFiles() {
	}

	private void createAgentDirectory() {
	}

	private TeamscaleCredentials parseCredentials(String[] args) throws FatalInstallerError {
	}

}
