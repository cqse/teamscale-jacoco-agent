package eu.cqse.teamscale.jacoco.recorder;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class Main {

	// Project settings
	private static String projectDirectory;

	// Jacoco agent settings
	private static String jacocoAgentAddress;
	private static int jacocoAgentPort;

	// Teamscale settings
	private static String teamscaleApiBaseUrl;
	private static String buildUser;
	private static String buildUserAccessToken;
	private static String testerId;

	public static void main(final String[] args) throws InterruptedException {
		loadConfig();

		try {
			CoverageRecorder coverageRecorder = new CoverageRecorder();
			coverageRecorder.checkProjectRepository(projectDirectory);
			coverageRecorder.connect(jacocoAgentAddress, jacocoAgentPort);
			coverageRecorder.startRecording(teamscaleApiBaseUrl, buildUser, buildUserAccessToken, testerId);

            exit:
            while (true) {
                System.out.print("Command: ");
                Scanner scanner = new Scanner(System.in);
                String command = scanner.nextLine();
                switch (command) {
                    case "reset":
                        coverageRecorder.resetExecutionData();
                        break;
                    case "upload":
                        coverageRecorder.uploadRecording();
                        break;
                    case "stop":
                        coverageRecorder.stopRecording();
                        break exit;
                    case "exit":
                        coverageRecorder.exit();
                        break exit;
                }
            }
		} catch (GitAPIException | IOException e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private static void loadConfig() {
		Properties prop = new Properties();

		try (InputStream input = new FileInputStream("config.properties")) {
			// load a properties file
			prop.load(input);

			projectDirectory = prop.getProperty("projectDirectory");

			jacocoAgentAddress = prop.getProperty("jacocoAgentAddress");
			jacocoAgentPort = Integer.parseInt(prop.getProperty("jacocoAgentPort"));

			teamscaleApiBaseUrl = prop.getProperty("teamscaleApiBaseUrl");
			buildUser = prop.getProperty("buildUser");
			buildUserAccessToken = prop.getProperty("buildUserAccessToken");
			testerId = prop.getProperty("testerId");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}