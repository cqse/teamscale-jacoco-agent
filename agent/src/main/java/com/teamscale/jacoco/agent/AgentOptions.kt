/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent

import com.teamscale.client.TeamscaleServer
import com.teamscale.jacoco.agent.commandline.Validator
import com.teamscale.jacoco.agent.store.IXmlStore
import com.teamscale.jacoco.agent.store.UploadStoreException
import com.teamscale.jacoco.agent.store.file.TimestampedFileStore
import com.teamscale.jacoco.agent.store.upload.azure.AzureFileStorageConfig
import com.teamscale.jacoco.agent.store.upload.azure.AzureFileStorageUploadStore
import com.teamscale.jacoco.agent.store.upload.http.HttpUploadStore
import com.teamscale.jacoco.agent.store.upload.teamscale.TeamscaleUploadStore
import com.teamscale.jacoco.agent.testimpact.TestwiseCoverageAgent
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.FileSystemUtils
import com.teamscale.report.util.Predicate
import okhttp3.HttpUrl
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

/**
 * Parses agent command line options.
 */
class AgentOptions {

    /**
     * The original options passed to the agent.
     */
    /* package */
    /**
     * @see .originalOptionsString
     */
    var originalOptionsString: String? = null
        internal set

    /**
     * The directories and/or zips that contain all class files being profiled.
     */
    /* package */
    /**
     * @see .classDirectoriesOrZips
     */
    var classDirectoriesOrZips: List<File> = ArrayList()
        internal set

    /**
     * The logging configuration file.
     */
    /* package */
    /**
     * @see .loggingConfig
     */
    /**
     * @see .loggingConfig
     */
    var loggingConfig: Path? = null

    /**
     * The directory to write the XML traces to.
     */
    /* package */ internal var outputDirectory: Path? = null

    /**
     * The URL to which to upload coverage zips.
     */
    /* package */ internal var uploadUrl: HttpUrl? = null

    /**
     * Additional meta data files to upload together with the coverage XML.
     */
    /* package */ internal var additionalMetaDataFiles: List<Path> = ArrayList()

    /**
     * The interval in minutes for dumping XML data.
     */
    /* package */
    /**
     * @see .dumpIntervalInMinutes
     */
    var dumpIntervalInMinutes = 60L
        internal set

    /**
     * Whether to ignore duplicate, non-identical class files.
     */
    /* package */ internal var shouldIgnoreDuplicateClassFiles = true

    /**
     * Include patterns to pass on to JaCoCo.
     */
    /* package */ internal var jacocoIncludes: String? = null

    /**
     * Exclude patterns to pass on to JaCoCo.
     */
    /* package */ internal var jacocoExcludes: String? = null

    /**
     * Additional user-provided options to pass to JaCoCo.
     */
    /* package */ internal var additionalJacocoOptions = mutableListOf<Pair<String, String>>()

    /**
     * The teamscale server to which coverage should be uploaded.
     */
    /* package */
    /** @see .teamscaleServer
     */
    var teamscaleServerOptions = TeamscaleServer()
        internal set

    /**
     * The name of the environment variable that holds the test uniform path.
     */
    /* package */
    /**
     * Returns the name of the environment variable to read the test uniform path from.
     */
    var testEnvironmentVariableName: String? = null
        internal set

    /**
     * The port on which the HTTP server should be listening.
     */
    /* package */
    /**
     * Returns the port at which the http server should listen for test execution information or null if disabled.
     */
    var httpServerPort: Int? = null
        internal set

    /**
     * The configuration necessary to upload files to an azure file storage
     */
    /* package */ internal var azureFileStorageConfig = AzureFileStorageConfig()

    /**
     * Validates the options and throws an exception if they're not valid.
     */
    /* package */ internal val validator: Validator
        get() {
            val validator = Validator()

            validator.isTrue(
                !classDirectoriesOrZips.isEmpty() || useTestwiseCoverageMode(),
                "You must specify at least one directory or zip that contains class files"
            )
            for (path in classDirectoriesOrZips) {
                validator.isTrue(path.exists(), "Path '$path' does not exist")
                validator.isTrue(path.canRead(), "Path '$path' is not readable")
            }

            validator.ensure {
                requireNotNull(outputDirectory) { "You must specify an output directory" }
                FileSystemUtils.ensureDirectoryExists(outputDirectory!!.toFile())
            }

            if (loggingConfig != null) {
                validator.ensure {
                    require(Files.exists(loggingConfig)) { "The path provided for the logging configuration does not exist: $loggingConfig" }
                    require(Files.isRegularFile(loggingConfig)) { "The path provided for the logging configuration is not a file: $loggingConfig" }
                    require(Files.isReadable(loggingConfig!!)) { "The file provided for the logging configuration is not readable: $loggingConfig" }
                    require(
                        loggingConfig!!.toFile().extension.equals(
                            "xml",
                            ignoreCase = true
                        )
                    ) { "The logging configuration file must have the file extension .xml and be a valid XML file" }
                }
            }

            validator.isTrue(
                !useTestwiseCoverageMode() || uploadUrl == null,
                "'upload-url' option is " + "incompatible with Testwise coverage mode!"
            )

            validator.isFalse(
                uploadUrl == null && !additionalMetaDataFiles.isEmpty(),
                "You specified additional meta data files to be uploaded but did not configure an upload URL"
            )

            validator.isTrue(
                teamscaleServerOptions.hasAllRequiredFieldsNull() || teamscaleServerOptions.hasAllRequiredFieldsSet(),
                "You did provide some options prefixed with 'teamscale-', but not all required ones!"
            )

            validator.isTrue(
                azureFileStorageConfig.hasAllRequiredFieldsSet() || azureFileStorageConfig
                    .hasAllRequiredFieldsNull(),
                "If you want to upload data to an azure file storage you need to provide both " + "'azure-url' and 'azure-key' "
            )

            val configuredStores = Arrays
                .asList(
                    azureFileStorageConfig.hasAllRequiredFieldsSet(), teamscaleServerOptions.hasAllRequiredFieldsSet(),
                    uploadUrl != null
                ).filter { x -> x }

            validator.isTrue(
                configuredStores.size <= 1,
                "You cannot configure multiple upload stores, " + "such as a teamscale instance, upload url or azure file storage"
            )

            return validator
        }

    /** Sets output to none for normal mode and destination file in testwise coverage mode  */
    private val modeSpecificOptions: String
        get() {
            if (useTestwiseCoverageMode()) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS", Locale.US)
                return "sessionid=,destfile=" + File(
                    outputDirectory!!.toFile(),
                    "jacoco-" + dateFormat.format(Date()) + ".exec"
                ).absolutePath
            } else {
                return "output=none"
            }
        }

    /**
     * @see .jacocoIncludes
     *
     * @see .jacocoExcludes
     */
    val locationIncludeFilter: Predicate<String>
        get() = ClasspathWildcardIncludeFilter(jacocoIncludes, jacocoExcludes)

    /**
     * Returns the options to pass to the JaCoCo agent.
     */
    fun createJacocoAgentOptions(): String {
        val builder = StringBuilder(modeSpecificOptions)
        if (jacocoIncludes != null) {
            builder.append(",includes=").append(jacocoIncludes)
        }
        if (jacocoExcludes != null) {
            builder.append(",excludes=").append(jacocoExcludes)
        }

        for ((key, value) in additionalJacocoOptions) {
            builder.append(",").append(key).append("=").append(value)
        }

        return builder.toString()
    }

    /**
     * Returns in instance of the agent that was configured. Either an agent with interval based line-coverage dump or
     * the HTTP server is used.
     */
    @Throws(UploadStoreException::class)
    fun createAgent(): AgentBase {
        return if (useTestwiseCoverageMode()) {
            TestwiseCoverageAgent(this)
        } else {
            Agent(this)
        }
    }

    /**
     * Creates the store to use for the coverage XMLs.
     */
    @Throws(UploadStoreException::class)
    fun createStore(): IXmlStore {
        val fileStore = TimestampedFileStore(outputDirectory!!)
        if (uploadUrl != null) {
            return HttpUploadStore(fileStore, uploadUrl!!, additionalMetaDataFiles)
        }
        if (teamscaleServerOptions.hasAllRequiredFieldsSet()) {
            return TeamscaleUploadStore(fileStore, teamscaleServerOptions)
        }

        return if (azureFileStorageConfig.hasAllRequiredFieldsSet()) {
            AzureFileStorageUploadStore(
                fileStore, azureFileStorageConfig,
                additionalMetaDataFiles
            )
        } else fileStore

    }

    /**
     * @see .shouldIgnoreDuplicateClassFiles
     */
    fun shouldIgnoreDuplicateClassFiles(): Boolean {
        return shouldIgnoreDuplicateClassFiles
    }

    /** Returns whether the config indicates to use Test Impact mode.  */
    private fun useTestwiseCoverageMode(): Boolean {
        return httpServerPort != null || testEnvironmentVariableName != null
    }

    /** Whether coverage should be dumped in regular intervals.  */
    fun shouldDumpInIntervals(): Boolean {
        return dumpIntervalInMinutes > 0
    }
}
