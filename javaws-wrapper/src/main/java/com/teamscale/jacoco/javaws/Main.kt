package com.teamscale.jacoco.javaws

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/** Wraps javaws and adds the profiler via `-J-javaagent`.  */
class Main {

    /** Contains the actual logic to run the wrapper.  */
    @Throws(InterruptedException::class, ConfigurationException::class, IOException::class)
    private fun run(args: Array<String>, workingDirectory: Path) {
        val configFile = workingDirectory.resolve(PROPERTIES_FILENAME).toAbsolutePath()

        val properties: Properties
        try {
            properties = readProperties(configFile)
        } catch (e: IOException) {
            throw ConfigurationException("Unable to read config file $configFile")
        }

        val pathToJavaws = readProperty(properties, PROPERTY_JAVAWS, configFile)
        val additionalAgentArguments = readProperty(properties, PROPERTY_AGENT_ARGUMENTS, configFile)

        val agentArgument = buildAgentArgument(workingDirectory, additionalAgentArguments)
        val policyArgument = buildPolicyArgument(workingDirectory)

        val commandLine = ArrayList(Arrays.asList(*args))
        commandLine.add(0, pathToJavaws)
        commandLine.add(1, policyArgument)

        println("Running real javaws command: $commandLine")
        println("With environment variable $JAVA_TOOL_OPTIONS_VARIABLE=$agentArgument")

        val exitCode = Main.runCommand(
            commandLine,
            Collections.singletonMap(JAVA_TOOL_OPTIONS_VARIABLE, agentArgument)
        )
        System.exit(exitCode)
    }

    private fun buildPolicyArgument(workingDirectory: Path): String {
        val policyFile = workingDirectory.resolve("agent.policy")
        return "-J-Djava.security.policy=" + normalizePath(policyFile)
    }

    /** Thrown if reading the config file fails.  */
    class ConfigurationException : Exception {

        constructor(message: String, cause: Throwable) : super(message, cause) {}

        constructor(message: String) : super(message) {}

        companion object {

            private val serialVersionUID = 1L
        }

    }

    companion object {

        /** Visible for testing.  */
        /* package */ internal val PROPERTY_AGENT_ARGUMENTS = "agentArguments"
        /** Visible for testing.  */
        /* package */ internal val PROPERTY_JAVAWS = "javaws"
        /** Visible for testing.  */
        /* package */ internal val PROPERTIES_FILENAME = "javaws.properties"
        private val JAVA_TOOL_OPTIONS_VARIABLE = "JAVA_TOOL_OPTIONS"

        /** Entry point.  */
        @Throws(
            InterruptedException::class,
            ConfigurationException::class,
            IOException::class,
            URISyntaxException::class
        )
        @JvmStatic
        fun main(args: Array<String>) {
            val jarFileUri = Main::class.java.protectionDomain.codeSource.location.toURI()
            // jar file is located inside the lib folder. Config files are one level higher
            val workingDirectory = Paths.get(jarFileUri).parent.parent

            if (args.size == 1 && args[0].equals("install", ignoreCase = true) || args[0].equals(
                    "uninstall",
                    ignoreCase = true
                )
            ) {
                handleInstallation(args, workingDirectory)
                return
            }

            Main().run(args, workingDirectory)
        }

        private fun handleInstallation(args: Array<String>, workingDirectory: Path) {
            try {
                val installation = WindowsInstallation(workingDirectory)

                when (args[0]) {
                    "install" -> installation.install()
                    "uninstall" -> installation.uninstall()
                }
            } catch (e: WindowsInstallation.InstallationException) {
                System.err.println("ERROR: " + e.message)
                e.printStackTrace()
                System.exit(1)
            }

        }

        /**
         * Runs the given command line and returns the exit code. Stdout and Stderr are
         * redirected to System.out/System.err.
         */
        @Throws(IOException::class, InterruptedException::class)
        fun runCommand(commandLine: List<String>, environmentVariables: Map<String, String>): Int {
            val builder = ProcessBuilder(commandLine)
            builder.environment().putAll(environmentVariables)
            val process = builder.inheritIO().start()
            return process.waitFor()
        }

        @Throws(ConfigurationException::class)
        private fun readProperty(properties: Properties, property: String, configFile: Path): String {
            val value = properties.getProperty(property, null)
                ?: throw ConfigurationException("Missing property `$property` in config file $configFile")
            return value
        }

        @Throws(IOException::class, ConfigurationException::class)
        private fun buildAgentArgument(workingDirectory: Path, additionalAgentArguments: String?): String {
            val agentJarPath = normalizePath(workingDirectory.resolve("agent.jar"))

            val tempDirectory = Files.createTempDirectory("javaws-classdumpdir")
            // we explicitly don't delete the temp directory because the javaws process will
            // exit before the actual application exits and the dir needs to be present or
            // JaCoCo will just crash
            // However, the files are created in the system's temp directory so they are
            // cleared up by the OS later in most cases
            val tempDirectoryPath = normalizePath(tempDirectory)

            if (additionalAgentArguments.isNullOrBlank()) {
                throw ConfigurationException("You must provide additional mandatory agent arguments." + " At least the dump interval and a method for storing the traces must be specified")
            }

            return ("-javaagent:" + agentJarPath + "=class-dir=" + tempDirectoryPath + ",jacoco-classdumpdir="
                    + tempDirectoryPath + "," + additionalAgentArguments)
        }

        /**
         * We normalize all paths to forward slashes to avoid problems with backward
         * slashes and escaping under Windows. Forward slashed paths still work under
         * Windows.
         */
        private fun normalizePath(path: Path): String {
            return path.toAbsolutePath().toString().replace(File.separatorChar, '/')
        }

        @Throws(IOException::class, FileNotFoundException::class)
        private fun readProperties(configFile: Path): Properties {
            val properties = Properties()
            FileInputStream(configFile.toFile()).use { inputStream -> properties.load(inputStream) }
            return properties
        }
    }

}
