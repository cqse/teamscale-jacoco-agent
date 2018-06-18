/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.commandline.Validator;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.store.file.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.agent.store.upload.http.HttpUploadStore;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.CommitDescriptor;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.TeamscaleServer;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.TeamscaleUploadStore;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.collections.PairList;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.runtime.WildcardMatcher;
import org.jacoco.report.JavaNames;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Parses agent command line options.
 */
public class AgentOptions {

    /**
     * Thrown if option parsing fails.
     */
    public static class AgentOptionParseException extends Exception {

        /**
         * Serialization ID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Constructor.
         */
        public AgentOptionParseException(String message) {
            super(message);
        }

        /**
         * Constructor.
         */
        public AgentOptionParseException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * The original options passed to the agent.
     */
    private final String originalOptionsString;

    /**
     * The directories and/or zips that contain all class files being profiled.
     */
    private List<File> classDirectoriesOrZips = new ArrayList<>();

    /**
     * Include patterns to apply during JaCoCo's traversal of class files. If null
     * then everything is included.
     */
    private WildcardMatcher locationIncludeFilters = null;

    /**
     * Exclude patterns to apply during JaCoCo's traversal of class files. If null
     * then nothing is excluded.
     */
    private WildcardMatcher locationExcludeFilters = null;

    /**
     * The logging configuration file.
     */
    private Path loggingConfig = null;

    /**
     * The directory to write the XML traces to.
     */
    private Path outputDir = null;

    /**
     * The URL to which to upload coverage zips.
     */
    private HttpUrl uploadUrl = null;

    /**
     * Additional meta data files to upload together with the coverage XML.
     */
    private List<Path> additionalMetaDataFiles = new ArrayList<>();

    /**
     * The interval in minutes for dumping XML data.
     */
    private int dumpIntervalInMinutes = 60;

    /**
     * Whether to ignore duplicate, non-identical class files.
     */
    private boolean shouldIgnoreDuplicateClassFiles = true;

    /**
     * Include patterns to pass on to JaCoCo.
     */
    private String jacocoIncludes = null;

    /**
     * Exclude patterns to pass on to JaCoCo.
     */
    private String jacocoExcludes = null;

    /**
     * Additional user-provided options to pass to JaCoCo.
     */
    private PairList<String, String> additionalJacocoOptions = new PairList<>();

    /** The teamscale server to which coverage should be uploaded. */
    private TeamscaleServer teamscaleServer = new TeamscaleServer();

    /**
     * Parses the given command-line options.
     */
    public AgentOptions(String options) throws AgentOptionParseException {
        this.originalOptionsString = options;

        if (StringUtils.isEmpty(options)) {
            throw new AgentOptionParseException(
                    "No agent options given. You must at least provide an output directory (out)"
                            + " and a classes directory (class-dir)");
        }

        String[] optionParts = options.split(",");
        for (String optionPart: optionParts) {
            handleOption(optionPart);
        }

        validate();
    }

    /**
     * @see #originalOptionsString
     */
    public String getOriginalOptionsString() {
        return originalOptionsString;
    }

    /**
     * Validates the options and throws an exception if they're not valid.
     */
    private void validate() throws AgentOptionParseException {
        Validator validator = new Validator();

        validator.isFalse(getClassDirectoriesOrZips().isEmpty(),
                "You must specify at least one directory or zip that contains class files");
        for (File path: classDirectoriesOrZips) {
            validator.isTrue(path.exists(), "Path '" + path + "' does not exist");
            validator.isTrue(path.canRead(), "Path '" + path + "' is not readable");
        }

        validator.ensure(() -> {
            CCSMAssert.isNotNull(outputDir, "You must specify an output directory");
            FileSystemUtils.ensureDirectoryExists(outputDir.toFile());
        });

        if (loggingConfig != null) {
            validator.ensure(() -> {
                CCSMAssert.isTrue(Files.exists(loggingConfig),
                        "The path provided for the logging configuration does not exist: " + loggingConfig);
                CCSMAssert.isTrue(Files.isRegularFile(loggingConfig),
                        "The path provided for the logging configuration is not a file: " + loggingConfig);
                CCSMAssert.isTrue(Files.isReadable(loggingConfig),
                        "The file provided for the logging configuration is not readable: " + loggingConfig);
                CCSMAssert.isTrue(FileSystemUtils.getFileExtension(loggingConfig.toFile()).equalsIgnoreCase("xml"),
                        "The logging configuration file must have the file extension .xml and be a valid XML file");
            });
        }

        validator.isFalse(uploadUrl == null && !additionalMetaDataFiles.isEmpty(),
                "You specified additional meta data files to be uploaded but did not configure an upload URL");

        validator.isTrue(teamscaleServer.hasAllFieldsNull() || teamscaleServer.hasAllFieldsSet(),
                "You did provide some options prefixed with 'teamscale-', but all required ones!");

        if (!validator.isValid()) {
            throw new AgentOptionParseException("Invalid options given: " + validator.getErrorMessage());
        }
    }

    /**
     * Parses and stores the given option in the format <code>key=value</code>.
     */
    private void handleOption(String optionPart) throws AgentOptionParseException {
        String[] keyAndValue = optionPart.split("=", 2);
        if (keyAndValue.length < 2) {
            throw new AgentOptionParseException("Got an option without any value: " + optionPart);
        }

        String key = keyAndValue[0];
        String value = keyAndValue[1];

        switch (key.toLowerCase()) {
            case "logging-config":
                loggingConfig = parsePath(key, value);
                break;
            case "interval":
                try {
                    dumpIntervalInMinutes = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new AgentOptionParseException("Non-numeric value given for option 'interval'");
                }
                break;
            case "out":
                outputDir = parsePath(key, value);
                break;
            case "upload-url":
                uploadUrl = parseUrl(value);
                if (uploadUrl == null) {
                    throw new AgentOptionParseException("Invalid URL given for option 'upload-url'");
                }
                break;
            case "upload-metadata":
                try {
                    additionalMetaDataFiles = CollectionUtils.map(splitMultiOptionValue(value), Paths::get);
                } catch (InvalidPathException e) {
                    throw new AgentOptionParseException("Invalid path given for option 'upload-metadata'");
                }
                break;
            case "ignore-duplicates":
                shouldIgnoreDuplicateClassFiles = Boolean.parseBoolean(value);
                break;
            case "includes":
                jacocoIncludes = value.replaceAll(";", ":");
                locationIncludeFilters = new WildcardMatcher(value);
                break;
            case "excludes":
                jacocoExcludes = value.replaceAll(";", ":");
                locationExcludeFilters = new WildcardMatcher(value);
                break;
            case "class-dir":
                classDirectoriesOrZips = CollectionUtils.map(splitMultiOptionValue(value), File::new);
                break;
            case "teamscale-server-url":
                teamscaleServer.url = parseUrl(value);
                if (teamscaleServer.url == null) {
                    throw new AgentOptionParseException("Invalid URL " + value + " given for option 'teamscale-server-url'!");
                }
                break;
            case "teamscale-project":
                teamscaleServer.project = value;
                break;
            case "teamscale-user":
                teamscaleServer.userName = value;
                break;
            case "teamscale-access-token":
                teamscaleServer.userAccessToken = value;
                break;
            case "teamscale-partition":
                teamscaleServer.partition = value;
                break;
            case "teamscale-commit":
                teamscaleServer.commit = parseCommit(value);
                break;
            case "teamscale-message":
                teamscaleServer.message = value;
                break;
            default:
                if (key.toLowerCase().startsWith("jacoco-")) {
                    additionalJacocoOptions.add(key.substring(7), value);
                    break;
                }

                throw new AgentOptionParseException("Unknown option: " + key);
        }
    }

    /**
     * Parses the given value as a {@link Path}.
     */
    private static Path parsePath(String optionName, String value) throws AgentOptionParseException {
        try {
            return Paths.get(value);
        } catch (InvalidPathException e) {
            throw new AgentOptionParseException("Invalid path given for option " + optionName + ": " + value, e);
        }
    }

    /**
     * Parses the given value as a URL or returns <code>null</code> if that fails.
     */
    private static HttpUrl parseUrl(String value) {
        // default to HTTP if no scheme is given
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }

        return HttpUrl.parse(value);
    }


    /**
     * Parses the the string representation of a commit to a  {@link CommitDescriptor} object.
     * <p>
     * The expected format is "branch:timestamp".
     */
    private static CommitDescriptor parseCommit(String commit) throws AgentOptionParseException {
        String[] split = commit.split(":");
        if (split.length != 2) {
            throw new AgentOptionParseException("Invalid commit given " + commit);
        }
        return new CommitDescriptor(split[0], split[1]);
    }

    /**
     * Splits the given value at semicolons.
     */
    private static List<String> splitMultiOptionValue(String value) {
        return Arrays.asList(value.split(";"));
    }

    /**
     * Returns the options to pass to the JaCoCo agent.
     */
    public String createJacocoAgentOptions() {
        StringBuilder builder = new StringBuilder("output=none");
        if (jacocoIncludes != null) {
            builder.append(",includes=").append(jacocoIncludes);
        }
        if (jacocoExcludes != null) {
            builder.append(",excludes=").append(jacocoExcludes);
        }

        additionalJacocoOptions.forEach((key, value) -> {
            builder.append(",").append(key).append("=").append(value);
        });

        return builder.toString();
    }

    /**
     * @see #classDirectoriesOrZips
     */
    public List<File> getClassDirectoriesOrZips() {
        return classDirectoriesOrZips;
    }

    /**
     * @see #locationIncludeFilters
     * @see #locationExcludeFilters
     */
    public Predicate<String> getLocationIncludeFilter() {
        return path -> {
            String className = getClassName(path);
            // first check includes
            if (locationIncludeFilters != null && !locationIncludeFilters.matches(className)) {
                return false;
            }
            // if they match, check excludes
            return locationExcludeFilters == null || !locationExcludeFilters.matches(className);
        };
    }

    /**
     * Creates the store to use for the coverage XMLs.
     */
    public IXmlStore createStore() {
        TimestampedFileStore fileStore = new TimestampedFileStore(outputDir);
        if (uploadUrl != null) {
            return new HttpUploadStore(fileStore, uploadUrl, additionalMetaDataFiles);
        }
        if (teamscaleServer.hasAllFieldsSet()) {
            return new TeamscaleUploadStore(fileStore, teamscaleServer);
        }
        return fileStore;

    }

    /**
     * @see #dumpIntervalInMinutes
     */
    public int getDumpIntervalInMinutes() {
        return dumpIntervalInMinutes;
    }

    /**
     * @see #shouldIgnoreDuplicateClassFiles
     */
    public boolean shouldIgnoreDuplicateClassFiles() {
        return shouldIgnoreDuplicateClassFiles;
    }

    /**
     * @see #loggingConfig
     */
    public Path getLoggingConfig() {
        return loggingConfig;
    }

    /**
     * @see #loggingConfig
     */
    public void setLoggingConfig(Path loggingConfig) {
        this.loggingConfig = loggingConfig;
    }

    /**
     * Returns the normalized class name of the given class file's path.
     */
    /* package */
    static String getClassName(String path) {
        String[] parts = FileSystemUtils.normalizeSeparators(path).split("@");
        if (parts.length == 0) {
            return "";
        }

        String pathInsideJar = parts[parts.length - 1];
        String pathWithoutExtension = StringUtils.removeLastPart(pathInsideJar, '.');
        return new JavaNames().getQualifiedClassName(pathWithoutExtension);
    }

}
