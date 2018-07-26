package org.junit.platform.console.options;

import java.io.File;
import java.nio.file.Path;

import eu.cqse.teamscale.client.CommitDescriptor;
import org.junit.platform.console.shadow.joptsimple.OptionParser;
import org.junit.platform.console.shadow.joptsimple.OptionSet;
import org.junit.platform.console.shadow.joptsimple.OptionSpec;
import org.junit.platform.console.shadow.joptsimple.util.PathConverter;
import org.junit.platform.engine.discovery.ClassNameFilter;

import static java.util.Arrays.asList;

class CustomAvailableOptions {

	private static final String CP_OPTION = "cp";

	private final OptionParser parser = new OptionParser();

	// General options
	private final OptionSpec<Void> help;
	private final OptionSpec<Void> disableAnsiColors;
	private final OptionSpec<Path> additionalClasspathEntries;

	private final OptionSpec<Details> details;
	private final OptionSpec<Theme> theme;

	// Reports
	private final OptionSpec<Path> reportsDir;

	// Selectors
	private final OptionSpec<Path> selectedClasspathEntries;
	private final OptionSpec<String> selectedFiles;
	private final OptionSpec<String> selectedDirectories;
	private final OptionSpec<String> selectedPackages;
	private final OptionSpec<String> selectedClasses;
	private final OptionSpec<String> selectedMethods;
	private final OptionSpec<String> selectedClasspathResources;

	// Filters
	private final OptionSpec<String> includeClassNamePattern;
	private final OptionSpec<String> excludeClassNamePattern;
	private final OptionSpec<String> includePackage;
	private final OptionSpec<String> excludePackage;
	private final OptionSpec<String> includeTag;
	private final OptionSpec<String> excludeTag;
	private final OptionSpec<String> includeEngine;
	private final OptionSpec<String> excludeEngine;

	// Teamscale
	private final OptionSpec<String> url;
	private final OptionSpec<String> project;
	private final OptionSpec<String> userName;
	private final OptionSpec<String> userAccessToken;
	private final OptionSpec<String> partition;

	//    private final OptionSpec<String> baseline;
	private final OptionSpec<String> end;

	private final OptionSpec<Void> runAllTests;

	CustomAvailableOptions() {

		// --- General Purpose -------------------------------------------------

		help = parser.acceptsAll(asList("h", "help"), //
				"Display help information.");

		disableAnsiColors = parser.accepts("disable-ansi-colors",
				"Disable ANSI colors in output (not supported by all terminals).");

		additionalClasspathEntries = parser.acceptsAll(asList(CP_OPTION, "classpath", "class-path"), //
				"Provide additional classpath entries -- for example, for adding engines and their dependencies. "
						+ "This option can be repeated.") //
				.withRequiredArg() //
				.withValuesConvertedBy(new PathConverter()) //
				.withValuesSeparatedBy(File.pathSeparatorChar) //
				.describedAs("path1" + File.pathSeparator + "path2" + File.pathSeparator + "...");

		// --- Reports ---------------------------------------------------------

		reportsDir = parser.accepts("reports-dir", //
				"Enable report output into a specified local directory (will be created if it does not exist).") //
				.withRequiredArg() //
				.withValuesConvertedBy(new PathConverter());

		details = parser.accepts("details",
				"Select an output details mode for when tests are executed. Use one of: " + asList(Details.values())
						+ ". If '" + Details.NONE + "' is selected, then only the summary and test failures are shown.") //
				.withRequiredArg() //
				.ofType(Details.class) //
				.withValuesConvertedBy(new DetailsConverter()) //
				.defaultsTo(CommandLineOptions.DEFAULT_DETAILS);

		theme = parser.accepts("details-theme",
				"Select an output details tree theme for when tests are executed. Use one of: " + asList(Theme.values())) //
				.withRequiredArg() //
				.ofType(Theme.class) //
				.withValuesConvertedBy(new ThemeConverter()) //
				.defaultsTo(CommandLineOptions.DEFAULT_THEME);
		// --- Selectors -------------------------------------------------------

		selectedClasspathEntries = parser.acceptsAll(asList("scan-class-path", "scan-classpath"), //
				"Scan all directories on the classpath or explicit classpath roots. " //
						+ "Without arguments, only directories on the system classpath as well as additional classpath " //
						+ "entries supplied via -" + CP_OPTION + " (directories and JAR files) are scanned. " //
						+ "Explicit classpath roots that are not on the classpath will be silently ignored. " //
						+ "This option can be repeated.") //
				.withOptionalArg() //
				.withValuesConvertedBy(new PathConverter()) //
				.withValuesSeparatedBy(File.pathSeparatorChar) //
				.describedAs("path1" + File.pathSeparator + "path2" + File.pathSeparator + "...");

		selectedFiles = parser.acceptsAll(asList("f", "select-file"), //
				"Select a file for test discovery. This option can be repeated.") //
				.withRequiredArg();

		selectedDirectories = parser.acceptsAll(asList("d", "select-directory"), //
				"Select a directory for test discovery. This option can be repeated.") //
				.withRequiredArg();

		selectedPackages = parser.acceptsAll(asList("p", "select-package"), //
				"Select a package for test discovery. This option can be repeated.") //
				.withRequiredArg();

		selectedClasses = parser.acceptsAll(asList("c", "select-class"), //
				"Select a class for test discovery. This option can be repeated.") //
				.withRequiredArg();

		selectedMethods = parser.acceptsAll(asList("m", "select-method"), //
				"Select a method for test discovery. This option can be repeated.") //
				.withRequiredArg();

		selectedClasspathResources = parser.acceptsAll(asList("r", "select-resource"), //
				"Select a classpath resource for test discovery. This option can be repeated.") //
				.withRequiredArg();

		// --- Filters ---------------------------------------------------------

		includeClassNamePattern = parser.acceptsAll(asList("n", "include-classname"),
				"Provide a regular expression to include only classes whose fully qualified names match. " //
						+ "To avoid loading classes unnecessarily, the default pattern only includes class " //
						+ "names that end with \"Test\" or \"Tests\". " //
						+ "When this option is repeated, all patterns will be combined using OR semantics.") //
				.withRequiredArg() //
				.defaultsTo(ClassNameFilter.STANDARD_INCLUDE_PATTERN);
		excludeClassNamePattern = parser.acceptsAll(asList("N", "exclude-classname"),
				"Provide a regular expression to exclude those classes whose fully qualified names match. " //
						+ "When this option is repeated, all patterns will be combined using OR semantics.") //
				.withRequiredArg();

		includePackage = parser.accepts("include-package",
				"Provide a package to be included in the test run. This option can be repeated.") //
				.withRequiredArg();
		excludePackage = parser.accepts("exclude-package",
				"Provide a package to be excluded from the test run. This option can be repeated.") //
				.withRequiredArg();

		includeTag = parser.acceptsAll(asList("t", "include-tag"),
				"Provide a tag or tag expression to include only tests whose tags match. " + //
						"When this option is repeated, all patterns will be combined using OR semantics.") //
				.withRequiredArg();
		excludeTag = parser.acceptsAll(asList("T", "exclude-tag"),
				"Provide a tag or tag expression to exclude those tests whose tags match. " + //
						"When this option is repeated, all patterns will be combined using OR semantics.") //
				.withRequiredArg();

		includeEngine = parser.acceptsAll(asList("e", "include-engine"),
				"Provide the ID of an engine to be included in the test run. This option can be repeated.") //
				.withRequiredArg();
		excludeEngine = parser.acceptsAll(asList("E", "exclude-engine"),
				"Provide the ID of an engine to be excluded from the test run. This option can be repeated.") //
				.withRequiredArg();


		// --- Teamscale parameters --------------------------------------------
		url = parser.accepts("url",
				"Url of the teamscale server")
				.withRequiredArg();

		project = parser.accepts("project",
				"Project ID of the teamscale project")
				.withRequiredArg();

		userName = parser.accepts("user",
				"The user name in teamscale")
				.withRequiredArg();

		userAccessToken = parser.accepts("access-token",
				"The users access token for Teamscale")
				.withRequiredArg();

		partition = parser.accepts("partition",
				"Partition of the tests")
				.withRequiredArg();

//        baseline = parser.accepts("baseline",
//                "The baseline commit")
//                .withRequiredArg();

		end = parser.accepts("end",
				"The end commit")
				.withRequiredArg();

		runAllTests = parser.acceptsAll(asList("all", "run-all-tests"),
				"Partition of the tests");

	}

	OptionParser getParser() {
		return parser;
	}

	CustomCommandLineOptions toCustomCommandLineOptions(OptionSet detectedOptions) {

		CustomCommandLineOptions result = new CustomCommandLineOptions();
		toJUnitCommandLineOptions(detectedOptions, result.toJUnitOptions());

		result.server.url = detectedOptions.valueOf(this.url);
		result.server.project = detectedOptions.valueOf(this.project);
		result.server.userName = detectedOptions.valueOf(this.userName);
		result.server.userAccessToken = detectedOptions.valueOf(this.userAccessToken);
		result.partition = detectedOptions.valueOf(this.partition);

		result.runAllTests = detectedOptions.has(this.runAllTests);

//        result.baselineCommit = CommitDescriptor.parse(detectedOptions.valueOf(this.baseline));
		result.endCommit = CommitDescriptor.parse(detectedOptions.valueOf(this.end));

		return result;
	}

	private void toJUnitCommandLineOptions(OptionSet detectedOptions, CommandLineOptions result) {

		// General Purpose
		result.setDisplayHelp(detectedOptions.has(this.help));
		result.setAnsiColorOutputDisabled(detectedOptions.has(this.disableAnsiColors));
		result.setDetails(detectedOptions.valueOf(this.details));
		result.setTheme(detectedOptions.valueOf(this.theme));
		result.setAdditionalClasspathEntries(detectedOptions.valuesOf(this.additionalClasspathEntries));

		// Reports
		result.setReportsDir(detectedOptions.valueOf(this.reportsDir));

		// Selectors
		result.setScanClasspath(detectedOptions.has(this.selectedClasspathEntries));
		result.setSelectedClasspathEntries(detectedOptions.valuesOf(this.selectedClasspathEntries));
		result.setSelectedFiles(detectedOptions.valuesOf(this.selectedFiles));
		result.setSelectedDirectories(detectedOptions.valuesOf(this.selectedDirectories));
		result.setSelectedPackages(detectedOptions.valuesOf(this.selectedPackages));
		result.setSelectedClasses(detectedOptions.valuesOf(this.selectedClasses));
		result.setSelectedMethods(detectedOptions.valuesOf(this.selectedMethods));
		result.setSelectedClasspathResources(detectedOptions.valuesOf(this.selectedClasspathResources));

		// Filters
		result.setIncludedClassNamePatterns(detectedOptions.valuesOf(this.includeClassNamePattern));
		result.setExcludedClassNamePatterns(detectedOptions.valuesOf(this.excludeClassNamePattern));
		result.setIncludedPackages(detectedOptions.valuesOf(this.includePackage));
		result.setExcludedPackages(detectedOptions.valuesOf(this.excludePackage));
		result.setIncludedTagExpressions(detectedOptions.valuesOf(this.includeTag));
		result.setExcludedTagExpressions(detectedOptions.valuesOf(this.excludeTag));
		result.setIncludedEngines(detectedOptions.valuesOf(this.includeEngine));
		result.setExcludedEngines(detectedOptions.valuesOf(this.excludeEngine));
	}

}

