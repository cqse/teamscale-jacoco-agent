/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent

import com.teamscale.client.CommitDescriptor
import com.teamscale.report.util.AntPatternUtils
import com.teamscale.report.util.FileSystemUtils
import com.teamscale.report.util.ILogger
import okhttp3.HttpUrl
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.jar.JarInputStream

/**
 * Parses agent command line options.
 */
class AgentOptionsParser(
    /** Logger.  */
    private val logger: ILogger
) {

    /**
     * Parses the given command-line options.
     */
    /* package */ @Throws(AgentOptionParseException::class)
    internal fun parse(optionsString: String?): AgentOptions {
        if (optionsString.isNullOrBlank()) {
            throw AgentOptionParseException(
                "No agent options given. You must at least provide an output directory (out)" + " and a classes directory (class-dir)"
            )
        }

        val options = AgentOptions()
        options.originalOptionsString = optionsString

        val optionParts = optionsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (optionPart in optionParts) {
            handleOption(options, optionPart)
        }

        val validator = options.validator
        if (!validator.isValid) {
            throw AgentOptionParseException("Invalid options given: " + validator.errorMessage)
        }
        return options
    }

    /**
     * Parses and stores the given option in the format `key=value`.
     */
    @Throws(AgentOptionParseException::class)
    private fun handleOption(options: AgentOptions, optionPart: String) {
        val keyAndValue = optionPart.split("=".toRegex(), 2).toTypedArray()
        if (keyAndValue.size < 2) {
            throw AgentOptionParseException("Got an option without any value: $optionPart")
        }

        val key = keyAndValue[0].toLowerCase()
        var value = keyAndValue[1]

        // Remove quotes, which may be used to pass arguments with spaces via the command line
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length - 1)
        }

        if (key.startsWith("jacoco-")) {
            options.additionalJacocoOptions.add(Pair(key.substring(7), value))
            return
        } else if (key.startsWith("teamscale-") && handleTeamscaleOptions(options, key, value)) {
            return
        } else if (handleHttpServerOptions(options, key, value)) {
            return
        } else if (key.startsWith("azure-") && handleAzureFileStorageOptions(options, key, value)) {
            return
        } else if (handleAgentOptions(options, key, value)) {
            return
        }
        throw AgentOptionParseException("Unknown option: $key")
    }

    /**
     * Handles all command line options for the agent without special prefix.
     *
     * @return true if it has successfully process the given option.
     */
    @Throws(AgentOptionParseException::class)
    private fun handleAgentOptions(options: AgentOptions, key: String, value: String): Boolean {
        when (key) {
            "config-file" -> {
                readConfigFromFile(options, parseFile(key, value))
                return true
            }
            "logging-config" -> {
                options.loggingConfig = parsePath(key, value)
                return true
            }
            "interval" -> {
                try {
                    options.dumpIntervalInMinutes = Integer.parseInt(value)
                } catch (e: NumberFormatException) {
                    throw AgentOptionParseException("Non-numeric value given for option 'interval'")
                }

                return true
            }
            "out" -> {
                options.outputDirectory = parsePath(key, value)
                return true
            }
            "upload-url" -> {
                options.uploadUrl = (parseUrl(value))
                if (options.uploadUrl == null) {
                    throw AgentOptionParseException("Invalid URL given for option 'upload-url'")
                }
                return true
            }
            "upload-metadata" -> {
                try {
                    options.additionalMetaDataFiles = splitMultiOptionValue(value).map { Paths.get(it) }
                } catch (e: InvalidPathException) {
                    throw AgentOptionParseException("Invalid path given for option 'upload-metadata'", e)
                }

                return true
            }
            "ignore-duplicates" -> {
                options.shouldIgnoreDuplicateClassFiles = java.lang.Boolean.parseBoolean(value)
                return true
            }
            "includes" -> {
                options.jacocoIncludes = value.replace(";".toRegex(), ":")
                return true
            }
            "excludes" -> {
                options.jacocoExcludes = value.replace(";".toRegex(), ":")
                return true
            }
            "class-dir" -> {
                options.classDirectoriesOrZips = splitMultiOptionValue(value).map { singleValue ->
                    parseFile(
                        key,
                        singleValue
                    )
                }
                return true
            }
            else -> return false
        }
    }

    /**
     * Reads configuration parameters from the given file.
     * The expected format is basically the same as for the command line, but line breaks are also considered as
     * separators.
     * e.g.
     * class-dir=out
     * # Some comment
     * includes=test.*
     * excludes=third.party.*
     */
    @Throws(AgentOptionParseException::class)
    private fun readConfigFromFile(options: AgentOptions, configFile: File) {
        try {
            val configFileKeyValues = configFile.readLines()
            for (optionKeyValue in configFileKeyValues) {
                val trimmedOption = optionKeyValue.trim { it <= ' ' }
                if (trimmedOption.isEmpty() || trimmedOption.startsWith(COMMENT_PREFIX)) {
                    continue
                }
                handleOption(options, optionKeyValue)
            }
        } catch (e: FileNotFoundException) {
            throw AgentOptionParseException(
                "File " + configFile.absolutePath + " given for option 'config-file' not found!", e
            )
        } catch (e: IOException) {
            throw AgentOptionParseException(
                "An error occurred while reading the config file " + configFile.absolutePath + "!", e
            )
        }

    }

    /**
     * Handles all command line options prefixed with "teamscale-".
     *
     * @return true if it has successfully process the given option.
     */
    @Throws(AgentOptionParseException::class)
    private fun handleTeamscaleOptions(options: AgentOptions, key: String, value: String): Boolean {
        when (key) {
            "teamscale-server-url" -> {
                options.teamscaleServerOptions.url = parseUrl(value)
                if (options.teamscaleServerOptions.url == null) {
                    throw AgentOptionParseException(
                        "Invalid URL $value given for option 'teamscale-server-url'!"
                    )
                }
                return true
            }
            "teamscale-project" -> {
                options.teamscaleServerOptions.project = value
                return true
            }
            "teamscale-user" -> {
                options.teamscaleServerOptions.userName = value
                return true
            }
            "teamscale-access-token" -> {
                options.teamscaleServerOptions.userAccessToken = value
                return true
            }
            "teamscale-partition" -> {
                options.teamscaleServerOptions.partition = value
                return true
            }
            "teamscale-commit" -> {
                options.teamscaleServerOptions.commit = parseCommit(value)
                return true
            }
            "teamscale-commit-manifest-jar" -> {
                options.teamscaleServerOptions.commit = getCommitFromManifest(parseFile(key, value))
                return true
            }
            "teamscale-message" -> {
                options.teamscaleServerOptions.message = value
                return true
            }
            else -> return false
        }
    }

    /**
     * Handles all command-line options prefixed with 'azure-'
     *
     * @return true if it has successfully process the given option.
     */
    @Throws(AgentOptionParseException::class)
    private fun handleAzureFileStorageOptions(options: AgentOptions, key: String, value: String): Boolean {
        when (key) {
            "azure-url" -> {
                options.azureFileStorageConfig.url = parseUrl(value)
                if (options.azureFileStorageConfig.url == null) {
                    throw AgentOptionParseException("Invalid URL given for option 'upload-azure-url'")
                }
                return true
            }
            "azure-key" -> {
                options.azureFileStorageConfig.accessKey = value
                return true
            }
            else -> return false
        }
    }

    /**
     * Reads `Branch` and `Timestamp` entries from the given jar/war file and
     * builds a commit descriptor out of it.
     */
    @Throws(AgentOptionParseException::class)
    private fun getCommitFromManifest(jarFile: File): CommitDescriptor {
        try {
            JarInputStream(FileInputStream(jarFile)).use { jarStream ->
                val manifest = jarStream.manifest
                val branch = manifest.mainAttributes.getValue("Branch")
                val timestamp = manifest.mainAttributes.getValue("Timestamp")
                if (branch.isNullOrBlank()) {
                    throw AgentOptionParseException("No entry 'Branch' in MANIFEST!")
                } else if (timestamp.isNullOrBlank()) {
                    throw AgentOptionParseException("No entry 'Timestamp' in MANIFEST!")
                }
                logger.debug("Found commit $branch:$timestamp in file $jarFile")
                return CommitDescriptor(branch, timestamp)
            }
        } catch (e: IOException) {
            throw AgentOptionParseException("Reading jar " + jarFile.absolutePath + " failed!", e)
        }

    }

    /**
     * Handles all command line options prefixed with "http-server-".
     *
     * @return true if it has successfully process the given option.
     */
    @Throws(AgentOptionParseException::class)
    private fun handleHttpServerOptions(options: AgentOptions, key: String, value: String): Boolean {
        when (key) {
            "test-env" -> {
                options.testEnvironmentVariableName = value
                return true
            }
            "http-server-port" -> {
                try {
                    options.httpServerPort = Integer.parseInt(value)
                } catch (e: NumberFormatException) {
                    throw AgentOptionParseException(
                        "Invalid port number $value given for option 'http-server-port'!"
                    )
                }

                return true
            }
            else -> return false
        }
    }

    /**
     * Parses the given value as a [File].
     */
    /* package */ @Throws(AgentOptionParseException::class)
    internal fun parseFile(optionName: String, value: String): File {
        return parsePath(optionName, File("."), value).toFile()
    }

    /**
     * Parses the given value as a [Path].
     */
    /* package */ @Throws(AgentOptionParseException::class)
    internal fun parsePath(optionName: String, value: String): Path {
        return parsePath(optionName, File("."), value)
    }

    /**
     * Parses the given value as a [File].
     */
    /* package */ @Throws(AgentOptionParseException::class)
    internal fun parseFile(optionName: String, workingDirectory: File, value: String): File {
        return parsePath(optionName, workingDirectory, value).toFile()
    }

    /**
     * Parses the given value as a [Path].
     */
    /* package */ @Throws(AgentOptionParseException::class)
    internal fun parsePath(optionName: String, workingDirectory: File, value: String): Path {
        if (isPathWithPattern(value)) {
            return parseFileFromPattern(workingDirectory, optionName, value)
        }
        try {
            return workingDirectory.toPath().resolve(Paths.get(value))
        } catch (e: InvalidPathException) {
            throw AgentOptionParseException("Invalid path given for option $optionName: $value", e)
        }

    }

    /** Parses the value as a ant pattern to a file or directory. */
    @Throws(AgentOptionParseException::class)
    private fun parseFileFromPattern(workingDirectory: File, optionName: String, value: String): Path {
        val (baseDir, pattern) = splitIntoBaseDirAndPattern(value)

        val workingDir = workingDirectory.absoluteFile
        val basePath = workingDir.resolve(baseDir).normalize().absoluteFile

        val pathMatcher = AntPatternUtils.convertPattern(pattern, false)
        val filter = { path: File ->
            pathMatcher
                .matcher(FileSystemUtils.normalizeSeparators(path.relativeTo(basePath).toString())).matches()
        }

        val matchingPaths: List<File>
        try {
            matchingPaths = basePath.walk().filter(filter).toList().sorted()
        } catch (e: IOException) {
            throw AgentOptionParseException(
                "Could not recursively list files in directory $basePath in order to resolve pattern $pattern given for option $optionName",
                e
            )
        }

        if (matchingPaths.isEmpty()) {
            throw AgentOptionParseException(
                "Invalid path given for option " + optionName + ": " + value + ". The pattern " + pattern +
                        " did not match any files in " + basePath.absolutePath + "!"
            )
        } else if (matchingPaths.size > 1) {
            logger.warn(
                "Multiple files match the pattern $pattern in " + basePath
                    .toString() + " for option " + optionName + "! " +
                        "The first one is used, but consider to adjust the " +
                        "pattern to match only one file. Candidates are: " + matchingPaths
                    .map { it.relativeTo(basePath) }.joinToString(", ") { it.toString() }
            )
        }
        val path = matchingPaths[0].normalize()
        logger.info("Using file $path for option $optionName")

        return Paths.get(path.toURI())
    }

    /**
     * Splits the path into a base dir, a the directory-prefix of the path that does not contain any ? or *
     * placeholders, and a pattern suffix.
     * We need to replace the pattern characters with stand-ins, because ? and * are not allowed as path characters on windows.
     */
    private fun splitIntoBaseDirAndPattern(value: String): Pair<String, String> {
        val pathWithArtificialPattern = value.replace("?", QUESTION_REPLACEMENT).replace("*", ASTERISK_REPLACEMENT)
        val pathWithPattern = Paths.get(pathWithArtificialPattern)
        var baseDir: Path? = pathWithPattern
        while (isPathWithArtificialPattern(baseDir!!.toString())) {
            baseDir = baseDir.parent
            if (baseDir == null) {
                return Pair("", value)
            }
        }
        val pattern = baseDir.relativize(pathWithPattern).toString().replace(QUESTION_REPLACEMENT, "?")
            .replace(ASTERISK_REPLACEMENT, "*")
        return Pair(baseDir.toString(), pattern)
    }

    companion object {

        /** Character which starts a comment in the config file.  */
        private val COMMENT_PREFIX = "#"

        /** Stand-in for the question mark operator.  */
        private val QUESTION_REPLACEMENT = "!@"

        /** Stand-in for the asterisk operator.  */
        private val ASTERISK_REPLACEMENT = "#@"

        /**
         * Parses the given command-line options.
         */
        @Throws(AgentOptionParseException::class)
        fun parse(optionsString: String, logger: ILogger): AgentOptions {
            return AgentOptionsParser(logger).parse(optionsString)
        }

        /** Returns whether the given path contains ant pattern characters (?,*).  */
        private fun isPathWithPattern(path: String): Boolean {
            return path.contains("?") || path.contains("*")
        }

        /** Returns whether the given path contains artificial pattern characters ([.QUESTION_REPLACEMENT], [.ASTERISK_REPLACEMENT]).  */
        private fun isPathWithArtificialPattern(path: String): Boolean {
            return path.contains(QUESTION_REPLACEMENT) || path.contains(ASTERISK_REPLACEMENT)
        }

        /**
         * Parses the given value as a URL or returns `null` if that fails.
         */
        private fun parseUrl(value: String): HttpUrl? {
            var fixedValue = value
            // default to HTTP if no scheme is given
            if (!fixedValue.startsWith("http://") && !fixedValue.startsWith("https://")) {
                fixedValue = "http://$fixedValue"
            }

            return HttpUrl.parse(fixedValue)
        }


        /**
         * Parses the the string representation of a commit to a  [CommitDescriptor] object.
         *
         *
         * The expected format is "branch:timestamp".
         */
        @Throws(AgentOptionParseException::class)
        private fun parseCommit(commit: String): CommitDescriptor {
            val split = commit.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size != 2) {
                throw AgentOptionParseException("Invalid commit given $commit")
            }
            return CommitDescriptor(split[0], split[1])
        }

        /**
         * Splits the given value at semicolons.
         */
        private fun splitMultiOptionValue(value: String): List<String> {
            return Arrays.asList(*value.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        }
    }
}
