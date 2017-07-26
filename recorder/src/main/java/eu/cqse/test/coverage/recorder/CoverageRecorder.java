package eu.cqse.test.coverage.recorder;

import eu.cqse.test.coverage.recorder.jacoco.ExecutionDataClient;
import eu.cqse.test.coverage.recorder.report.XMLReportGenerator;
import eu.cqse.test.coverage.recorder.upload.TeamscaleClient;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoverageRecorder {

	private File projectDirectory;
	private ExecutionDataClient client;
	private String branchName;
	private String timestamp;
	private String projectId;
	private String teamscaleBaseUrl;
	private String user;
	private String accessToken;
	private String testerId;

	void checkProjectRepository(String project) throws IOException, GitAPIException {
		System.out.println("Checking repository...");

		projectDirectory = new File(project);
		Git git = Git.init().setDirectory(projectDirectory).call();

		branchName = getCurrentBranchName(projectDirectory);
        projectId = getProjectIdFromBranch(branchName);
		System.out.println("Teamscale project: " + projectId);

		this.timestamp = getLastCommitTimestamp() + "000";
		System.out.println("Recording for " + branchName + " at " + timestamp);

		// Test for a clean workspace -> No working copy changes
		if (!git.status().call().isClean()) {
			System.out.println("Warning: You have uncommitted changes in your project!");
		}
	}

    private String getProjectIdFromBranch(String branchName) {
        Pattern p = Pattern.compile("cr/([0-9]+)");
        Matcher m = p.matcher(branchName);
        if (m.find()) {
            String issue = m.group(1);
            System.out.println("Detected Issue: " + issue);
            return "cr-" + issue;
        } else {
            String projectId = branchName.replace("/", "-");
            if (projectId.length() > 12) {
                projectId = projectId.substring(0, 12);
            }
            System.out.println("Warning: You have not checked out a cr branch!");
            return projectId;
        }
    }

    private static String getCurrentBranchName(File projectDirectory) throws IOException {
		Repository repo = new FileRepositoryBuilder().findGitDir(projectDirectory).build();
		return repo.getBranch();
	}

	void connect(String address, int port) throws IOException {
		// Connect to jacoco agent
		client = new ExecutionDataClient();
		System.out.println("Connecting to jacoco agent...");
		try {
			client.connectTo(address, port);
		} catch (ConnectException e) {
			throw new IllegalStateException("Jacoco agent is not running!");
		}
	}

	void startRecording(String teamscaleBaseUrl, String user, String accessToken, String testerId)
			throws IOException {
		this.teamscaleBaseUrl = teamscaleBaseUrl;
		this.user = user;
		this.accessToken = accessToken;
		this.testerId = testerId;
		System.out.println("Recording started...");
		System.out.println("Commands:");
		System.out.println("* (reset) to discard everything recorded until now");
		System.out.println("* (upload) to upload the recording (no reset)");
		System.out.println("* (exit) Immediately exit the tool WITHOUT uploading coverage");
		System.out.println("* (stop) (upload) and (exit) afterwards");
	}

	void resetExecutionData() throws IOException {
		client.resetExecutionData();
	}

	// Send a dump command and read the response
	private void dumpExecutionData() throws IOException {
		System.out.println("Generating report...");
		ExecutionDataWrapper data = new ExecutionDataWrapper(client.dumpExecutionData(false));

		// Convert the execution data to xml format
		byte[] report = createReportFromExecutionData(data.data);

		FileWriter writer = new FileWriter(new File("report.xml"));
		writer.write(new String(report));
		writer.close();

		// Upload xml report to Teamscale
		System.out.println("Uploading...");
		uploadReportToTeamscale(report, teamscaleBaseUrl, user, accessToken, testerId);
	}

	void uploadRecording() throws IOException, InterruptedException {
		dumpExecutionData();
	}

	void stopRecording() throws IOException, InterruptedException {
		dumpExecutionData();
		exit();
	}

	void exit() throws IOException, InterruptedException {
		client.closeConnection();
	}

	private byte[] createReportFromExecutionData(byte[] data) throws IOException {
		return new XMLReportGenerator(projectDirectory, data).create();
	}

	private void uploadReportToTeamscale(byte[] report, String teamscaleBaseUrl, String user,
			String accessToken, String testerId) throws IOException {
		String timestamp = branchName + ":" + this.timestamp;

		new TeamscaleClient(teamscaleBaseUrl, user, accessToken).upload(report, projectId, timestamp,
				"coverage_recorder_" + projectId + "_" + testerId);
	}

	private long getLastCommitTimestamp() throws IOException {
		try {
			Git git = Git.init().setDirectory(projectDirectory).call();
			Iterable<RevCommit> res = git.log().setMaxCount(1).call();
			return res.iterator().next().getCommitTime();
		} catch (GitAPIException e) {
			throw new IOException("Failed to get git timestamp", e);
		}
	}

	private static class ExecutionDataWrapper {
		byte[] data;

		ExecutionDataWrapper(byte[] data) {
			this.data = data;
		}
	}
}