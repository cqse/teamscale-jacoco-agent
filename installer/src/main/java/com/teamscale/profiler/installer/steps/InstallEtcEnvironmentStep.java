package com.teamscale.profiler.installer.steps;

import com.teamscale.profiler.installer.EnvironmentMap;
import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.PermissionError;
import com.teamscale.profiler.installer.TeamscaleCredentials;
import org.conqat.lib.commons.string.StringUtils;
import org.conqat.lib.commons.system.SystemUtils;

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
	private final EnvironmentMap environmentVariables;

	public InstallEtcEnvironmentStep(Path etcDirectory, EnvironmentMap environmentMap) {
		this.etcDirectory = etcDirectory;
		this.environmentVariables = environmentMap;
	}

	@Override
	public void install(TeamscaleCredentials credentials) throws FatalInstallerError {
		if (!SystemUtils.isLinux()) {
			return;
		}

		Path environmentFile = getEnvironmentFile();
		if (!Files.exists(environmentFile)) {
			System.err.println(
					environmentFile + " does not exist. Skipping system-wide registration of the profiler."
							+ "\nYou need to manually register the profiler for process that should be profiled by"
							+ " setting the following environment variables:"
							+ "\n\n" + environmentVariables.getEtcEnvironmentString() + "\n");
			return;
		}

		String content = "\n" + environmentVariables.getEtcEnvironmentString() + "\n";

		try {
			Files.write(environmentFile, content.getBytes(StandardCharsets.US_ASCII),
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
		if (!SystemUtils.isLinux()) {
			return;
		}

		Path environmentFile = getEnvironmentFile();
		if (!Files.exists(environmentFile)) {
			return;
		}

		try {
			List<String> lines = Files.readAllLines(environmentFile, StandardCharsets.US_ASCII);
			String newContent = removeProfilerVariables(lines);
			Files.write(environmentFile, newContent.getBytes(StandardCharsets.US_ASCII));
		} catch (IOException e) {
			errorReporter.report(new PermissionError("Failed to remove profiler from " + environmentFile + "." +
					" Please remove the relevant environment variables yourself." +
					" Otherwise, Java applications may crash.", e));
		}
	}

	private String removeProfilerVariables(List<String> linesWithoutNewline) {
		Set<String> linesToRemove = new HashSet<>(StringUtils.splitLinesAsList(
				environmentVariables.getEtcEnvironmentString(), false));
		return linesWithoutNewline.stream().filter(line -> !linesToRemove.contains(line))
				.collect(Collectors.joining("\n"));
	}
}
