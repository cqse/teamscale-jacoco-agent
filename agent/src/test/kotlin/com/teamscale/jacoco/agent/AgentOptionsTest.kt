package com.teamscale.jacoco.agent

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.function.Predicate

/** Tests the [AgentOptions].  */
class AgentOptionsTest {

    @get:Rule
    var testFolder = TemporaryFolder()

    private val agentOptionsParserWithDummyLogger: AgentOptionsParser
        get() = AgentOptionsParser(DummyLogger())

    @Before
    @Throws(IOException::class)
    fun setUp() {
        testFolder.create()
        testFolder.newFile("file_with_manifest1.jar")
        testFolder.newFolder("plugins")
        testFolder.newFolder("plugins", "inner")
        testFolder.newFile("plugins/some_other_file.jar")
        testFolder.newFile("plugins/file_with_manifest2.jar")
    }

    /** Tests include pattern matching.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testIncludePatternMatching() {
        assertThat(includeFilter("com.*")).accepts(
            "file.jar@com/foo/Bar.class", "file.jar@com/foo/Bar\$Goo.class",
            "file1.jar@goo/file2.jar@com/foo/Bar.class", "com/foo/Bar.class", "com.foo/Bar.class"
        )
        assertThat(includeFilter("com.*")).rejects(
            "foo/com/Bar.class", "com.class", "file.jar@com.class",
            "A\$com\$Bar.class"
        )
        assertThat(includeFilter("*com.*")).accepts(
            "file.jar@com/foo/Bar.class", "file.jar@com/foo/Bar\$Goo.class",
            "file1.jar@goo/file2.jar@com/foo/Bar.class", "com/foo/Bar.class", "foo/com/goo/Bar.class",
            "A\$com\$Bar.class", "src/com/foo/Bar.class"
        )
        assertThat(includeFilter("*com.*;*de.*"))
            .accepts("file.jar@com/foo/Bar.class", "file.jar@de/foo/Bar\$Goo.class")
        assertThat(excludeFilter("*com.*;*de.*"))
            .rejects("file.jar@com/foo/Bar.class", "file.jar@de/foo/Bar\$Goo.class")
        assertThat(includeFilter("*com.customer.*")).accepts(
            "C:\\client-daily\\client\\plugins\\com.customer.something.client_1.2.3.4.1234566778.jar@com/customer/something/SomeClass.class"
        )
    }

    /** Interval options test.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testIntervalOptions() {
        var agentOptions = agentOptionsParserWithDummyLogger.parse("out=.,class-dir=.")
        assertThat(agentOptions.dumpIntervalInMinutes).isEqualTo(60)
        agentOptions = agentOptionsParserWithDummyLogger.parse("out=.,class-dir=.,interval=0")
        assertThat(agentOptions.shouldDumpInIntervals()).isEqualTo(false)
        agentOptions = agentOptionsParserWithDummyLogger.parse("out=.,class-dir=.,interval=30")
        assertThat(agentOptions.shouldDumpInIntervals()).isEqualTo(true)
        assertThat(agentOptions.dumpIntervalInMinutes).isEqualTo(30)
    }

    /** Tests the options for uploading coverage to teamscale.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testTeamscaleUploadOptions() {
        val agentOptions = agentOptionsParserWithDummyLogger.parse(
            "out=.,class-dir=.," +
                    "teamscale-server-url=127.0.0.1," +
                    "teamscale-project=test," +
                    "teamscale-user=build," +
                    "teamscale-access-token=token," +
                    "teamscale-partition=\"Unit Tests\"," +
                    "teamscale-commit=default:HEAD," +
                    "teamscale-message=\"This is my message\""
        )

        val teamscaleServer = agentOptions.teamscaleServerOptions
        assertThat(teamscaleServer.url!!.toString()).isEqualTo("http://127.0.0.1/")
        assertThat(teamscaleServer.project).isEqualTo("test")
        assertThat(teamscaleServer.userName).isEqualTo("build")
        assertThat(teamscaleServer.userAccessToken).isEqualTo("token")
        assertThat(teamscaleServer.partition).isEqualTo("Unit Tests")
        assertThat(teamscaleServer.commit!!.toString()).isEqualTo("default:HEAD")
        assertThat(teamscaleServer.message).isEqualTo("This is my message")
    }

    /** Tests the options for the Test Impact mode.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testHttpServerOptions() {
        val agentOptions = agentOptionsParserWithDummyLogger.parse(
            "out=.,class-dir=.," +
                    "http-server-port=8081," +
                    "test-env=TEST"
        )
        assertThat(agentOptions.httpServerPort).isEqualTo(8081)
        assertThat(agentOptions.testEnvironmentVariableName).isEqualTo("TEST")
    }

    /** Tests the options for azure file storage upload.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testAzureFileStorageOptions() {
        val agentOptions = agentOptionsParserWithDummyLogger.parse(
            "out=.,class-dir=.," +
                    "azure-url=https://mrteamscaleshdev.file.core.windows.net/tstestshare/," +
                    "azure-key=Ut0BQ2OEvgQXGnNJEjxnaEULAYgBpAK9+HukeKSzAB4CreIQkl2hikIbgNe4i+sL0uAbpTrFeFjOzh3bAtMMVg=="
        )
        assertThat(agentOptions.azureFileStorageConfig.url.toString())
            .isEqualTo("https://mrteamscaleshdev.file.core.windows.net/tstestshare/")
        assertThat(agentOptions.azureFileStorageConfig.accessKey).isEqualTo(
            "Ut0BQ2OEvgQXGnNJEjxnaEULAYgBpAK9+HukeKSzAB4CreIQkl2hikIbgNe4i+sL0uAbpTrFeFjOzh3bAtMMVg=="
        )
    }

    /** Returns the include filter predicate for the given filter expression.  */
    @Throws(AgentOptionParseException::class)
    private fun includeFilter(filterString: String): Predicate<String> {
        val agentOptions = agentOptionsParserWithDummyLogger
            .parse("out=.,class-dir=.,includes=$filterString")
        return Predicate { string -> agentOptions.locationIncludeFilter.test(string) }
    }

    /** Returns the include filter predicate for the given filter expression.  */
    @Throws(AgentOptionParseException::class)
    private fun excludeFilter(filterString: String): Predicate<String> {
        val agentOptions = agentOptionsParserWithDummyLogger
            .parse("out=.,class-dir=.,excludes=$filterString")
        return Predicate { string -> agentOptions.locationIncludeFilter.test(string) }
    }

    /** Tests path resolution with absolute path.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testPathResolutionForAbsolutePath() {
        assertInputInWorkingDirectoryMatches(".", testFolder.root.absolutePath, "")
    }

    /** Tests path resolution with relative paths.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testPathResolutionForRelativePath() {
        assertInputInWorkingDirectoryMatches(".", ".", "")
        assertInputInWorkingDirectoryMatches("plugins", "../file_with_manifest1.jar", "file_with_manifest1.jar")
    }

    /** Tests path resolution with patterns and relative paths.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testPathResolutionWithPatternsAndRelativePaths() {
        assertInputInWorkingDirectoryMatches(".", "plugins/file_*.jar", "plugins/file_with_manifest2.jar")
        assertInputInWorkingDirectoryMatches(".", "*/file_*.jar", "plugins/file_with_manifest2.jar")
        assertInputInWorkingDirectoryMatches("plugins/inner", "..", "plugins")
        assertInputInWorkingDirectoryMatches("plugins/inner", "../s*", "plugins/some_other_file.jar")
    }

    /** Tests path resolution with patterns and absolute paths.  */
    @Test
    @Throws(AgentOptionParseException::class)
    fun testPathResolutionWithPatternsAndAbsolutePaths() {
        assertInputInWorkingDirectoryMatches(
            "plugins", testFolder.root.absolutePath + "/plugins/file_*.jar",
            "plugins/file_with_manifest2.jar"
        )
    }

    /** Tests path resolution with incorrect input.  */
    @Test
    fun testPathResolutionWithPatternErrorCases() {
        assertPathResolutionInWorkingDirFailsWith(
            ".",
            "**.war",
            "Invalid path given for option option-name: " + "**.war. The pattern **.war did not match any files in"
        )
    }

    @Throws(AgentOptionParseException::class)
    private fun assertInputInWorkingDirectoryMatches(workingDir: String, input: String, expected: String) {
        val workingDirectory = File(testFolder.root, workingDir)
        val actualFile = agentOptionsParserWithDummyLogger.parseFile("option-name", workingDirectory, input)
        val expectedFile = File(testFolder.root, expected)
        assertThat(getNormalizedPath(actualFile)).isEqualByComparingTo(getNormalizedPath(expectedFile))
    }

    /** Resolves the path to its absolute normalized path.  */
    private fun getNormalizedPath(file: File): Path {
        return file.absoluteFile.toPath().normalize()
    }

    private fun assertPathResolutionInWorkingDirFailsWith(workingDir: String, input: String, expectedMessage: String) {
        val workingDirectory = File(testFolder.root, workingDir)
        assertThatThrownBy { agentOptionsParserWithDummyLogger.parseFile("option-name", workingDirectory, input) }
            .isInstanceOf(AgentOptionParseException::class.java).hasMessageContaining(expectedMessage)
    }
}
