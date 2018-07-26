package org.junit.platform.console.options;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.cqse.teamscale.client.CommitDescriptor;
import eu.cqse.teamscale.client.Server;

public class CustomCommandLineOptions {

	public Server server = new Server();

	public String partition;

	public boolean runAllTests;

//    public CommitDescriptor baselineCommit;

	public CommitDescriptor endCommit;

	private CommandLineOptions commandLineOptions = new CommandLineOptions();

	public boolean isDisplayHelp() {
		return commandLineOptions.isDisplayHelp();
	}

	public void setDisplayHelp(boolean displayHelp) {
		commandLineOptions.setDisplayHelp(displayHelp);
	}

	public Details getDetails() {
		return commandLineOptions.getDetails();
	}

	public void setDetails(Details details) {
		this.commandLineOptions.setDetails(details);
	}

	public Theme getTheme() {
		return commandLineOptions.getTheme();
	}

	public void setTheme(Theme theme) {
		this.commandLineOptions.setTheme(theme);
	}

	public boolean isAnsiColorOutputDisabled() {
		return commandLineOptions.isAnsiColorOutputDisabled();
	}

	public void setAnsiColorOutputDisabled(boolean ansiColorOutputDisabled) {
		commandLineOptions.setAnsiColorOutputDisabled(ansiColorOutputDisabled);
	}
//
//    public boolean isScanModulepath() {
//        return commandLineOptions.isScanModulepath();
//    }
//
//    public void setScanModulepath(boolean scanModulepath) {
//        commandLineOptions.setScanModulepath(scanModulepath);
//    }

	public boolean isScanClasspath() {
		return commandLineOptions.isScanClasspath();
	}

	public void setScanClasspath(boolean scanClasspath) {
		commandLineOptions.setScanClasspath(scanClasspath);
	}

	public List<URI> getSelectedUris() {
		return commandLineOptions.getSelectedUris();
	}

	public void setSelectedUris(List<URI> selectedUris) {
		commandLineOptions.setSelectedUris(selectedUris);
	}

	public List<String> getSelectedFiles() {
		return commandLineOptions.getSelectedFiles();
	}

	public void setSelectedFiles(List<String> selectedFiles) {
		commandLineOptions.setSelectedFiles(selectedFiles);
	}

	public List<String> getSelectedDirectories() {
		return commandLineOptions.getSelectedDirectories();
	}

	public void setSelectedDirectories(List<String> selectedDirectories) {
		commandLineOptions.setSelectedDirectories(selectedDirectories);
	}
//
//    public List<String> getSelectedModules() {
//        return commandLineOptions.getSelectedModules();
//    }
//
//    public void setSelectedModules(List<String> selectedModules) {
//        commandLineOptions.setSel(selectedModules);
//    }

	public List<String> getSelectedPackages() {
		return commandLineOptions.getSelectedPackages();
	}

	public void setSelectedPackages(List<String> selectedPackages) {
		commandLineOptions.setSelectedPackages(selectedPackages);
	}

	public List<String> getSelectedClasses() {
		return commandLineOptions.getSelectedClasses();
	}

	public void setSelectedClasses(List<String> selectedClasses) {
		commandLineOptions.setSelectedClasses(selectedClasses);
	}

	public List<String> getSelectedMethods() {
		return commandLineOptions.getSelectedMethods();
	}

	public void setSelectedMethods(List<String> selectedMethods) {
		commandLineOptions.setSelectedMethods(selectedMethods);
	}

	public List<String> getSelectedClasspathResources() {
		return commandLineOptions.getSelectedClasspathResources();
	}

	public void setSelectedClasspathResources(List<String> selectedClasspathResources) {
		commandLineOptions.setSelectedClasspathResources(selectedClasspathResources);
	}

	public List<String> getIncludedClassNamePatterns() {
		return commandLineOptions.getIncludedClassNamePatterns();
	}

	public void setIncludedClassNamePatterns(List<String> includedClassNamePatterns) {
		commandLineOptions.setIncludedClassNamePatterns(includedClassNamePatterns);
	}

	public List<String> getExcludedClassNamePatterns() {
		return commandLineOptions.getExcludedClassNamePatterns();
	}

	public void setExcludedClassNamePatterns(List<String> excludedClassNamePatterns) {
		commandLineOptions.setExcludedClassNamePatterns(excludedClassNamePatterns);
	}

	public List<String> getIncludedPackages() {
		return commandLineOptions.getIncludedPackages();
	}

	public void setIncludedPackages(List<String> includedPackages) {
		commandLineOptions.setIncludedPackages(includedPackages);
	}

	public List<String> getExcludedPackages() {
		return commandLineOptions.getExcludedPackages();
	}

	public void setExcludedPackages(List<String> excludedPackages) {
		commandLineOptions.setExcludedPackages(excludedPackages);
	}

	public List<String> getIncludedEngines() {
		return commandLineOptions.getIncludedEngines();
	}

	public void setIncludedEngines(List<String> includedEngines) {
		commandLineOptions.setIncludedEngines(includedEngines);
	}

	public List<String> getExcludedEngines() {
		return commandLineOptions.getExcludedEngines();
	}

	public void setExcludedEngines(List<String> excludedEngines) {
		commandLineOptions.setExcludedEngines(excludedEngines);
	}

	public List<String> getIncludedTagExpressions() {
		return commandLineOptions.getIncludedTagExpressions();
	}

	public void setIncludedTagExpressions(List<String> includedTags) {
		commandLineOptions.setIncludedTagExpressions(includedTags);
	}

	public List<String> getExcludedTagExpressions() {
		return commandLineOptions.getExcludedTagExpressions();
	}

	public void setExcludedTagExpressions(List<String> excludedTags) {
		commandLineOptions.setExcludedTagExpressions(excludedTags);
	}

	public List<Path> getAdditionalClasspathEntries() {
		return commandLineOptions.getAdditionalClasspathEntries();
	}

	public void setAdditionalClasspathEntries(List<Path> additionalClasspathEntries) {
		commandLineOptions.setAdditionalClasspathEntries(additionalClasspathEntries);
	}

	public Optional<Path> getReportsDir() {
		return commandLineOptions.getReportsDir();
	}

	public void setReportsDir(Path reportsDir) {
		commandLineOptions.setReportsDir(reportsDir);
	}

	public List<Path> getSelectedClasspathEntries() {
		return commandLineOptions.getSelectedClasspathEntries();
	}

	public void setSelectedClasspathEntries(List<Path> selectedClasspathEntries) {
		commandLineOptions.setSelectedClasspathEntries(selectedClasspathEntries);
	}

	public Map<String, String> getConfigurationParameters() {
		return commandLineOptions.getConfigurationParameters();
	}

	public void setConfigurationParameters(Map<String, String> configurationParameters) {
		commandLineOptions.setConfigurationParameters(configurationParameters);
	}

	public CommandLineOptions toJUnitOptions() {
		return commandLineOptions;
	}
}
