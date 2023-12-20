package com.teamscale.profiler.installer.steps;

import com.teamscale.profiler.installer.JvmEnvironmentMap;
import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.PermissionError;
import com.teamscale.profiler.installer.TeamscaleCredentials;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** On Linux, registers the agent globally via environment variables set in /etc/environment */
public class InstallEtcEnvironmentStep implements IStep {

	private final Path etcDirectory;
	private final JvmEnvironmentMap environmentVariables;

	public InstallEtcEnvironmentStep(Path etcDirectory, JvmEnvironmentMap environmentMap) {
		this.etcDirectory = etcDirectory;
		this.environmentVariables = environmentMap;
	}

	@Override
	public boolean shouldNotRun() {
		return !SystemUtils.IS_OS_LINUX;
	}

	@Override
	public void install(TeamscaleCredentials credentials) throws FatalInstallerError {
		Path environmentFile = getEnvironmentFile();
		String etcEnvironmentAddition = String.join("\n", environmentVariables.getEtcEnvironmentLinesList());

		if (!Files.exists(environmentFile)) {
			System.err.println(
					environmentFile + " does not exist. Skipping system-wide registration of the profiler."
					+ "\nYou need to manually register the profiler for process that should be profiled by"
					+ " setting the following environment variables:"
					+ "\n\n" + etcEnvironmentAddition + "\n");
			return;
		}

		String content = "\n" + etcEnvironmentAddition + "\n";
		try {
			Files.writeString(environmentFile, content, StandardCharsets.US_ASCII,
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new PermissionError("Could not change contents of " + environmentFile, e);
		}
	}

	private Path getEnvironmentFile() {
		return etcDirectory.resolve("environment");
	}

	@Override
	public void uninstall(IUninstallErrorReporter errorReporter) {
		Path environmentFile = getEnvironmentFile();
		if (!Files.exists(environmentFile)) {
			return;
		}

		try {
			List<String> lines = Files.readAllLines(environmentFile, StandardCharsets.US_ASCII);
			String newContent = removeProfilerVariables(lines);
			Files.writeString(environmentFile, newContent, StandardCharsets.US_ASCII);
		} catch (IOException e) {
			errorReporter.report(new PermissionError("Failed to remove profiler from " + environmentFile + "." +
													 " Please remove the relevant environment variables yourself." +
													 " Otherwise, Java applications may crash.", e));
		}
	}

	private String removeProfilerVariables(List<String> linesWithoutNewline) {
		Set<String> linesToRemove = new HashSet<>(environmentVariables.getEtcEnvironmentLinesList());
		return linesWithoutNewline.stream().filter(line -> !linesToRemove.contains(line))
				.collect(Collectors.joining("\n"));
	}
}
