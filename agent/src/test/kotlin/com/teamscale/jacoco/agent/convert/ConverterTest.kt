package com.teamscale.jacoco.agent.convert

import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.conqat.lib.commons.test.CCSMTestCaseBase
import org.junit.Test

import java.io.File
import java.util.Collections

import org.assertj.core.api.Assertions.assertThat

/** Basic smoke test for the converter.  */
class ConverterTest : CCSMTestCaseBase() {

    /**
     * Ensures that running the converter on valid input does not yield any errors
     * and produces a coverage XML report.
     */
    @Test
    @Throws(Exception::class)
    fun testSmokeTest() {
        val execFile = useTestFile("coverage.exec")
        val classFile = useTestFile("TestClass.class")
        val outputFile = createTmpFile("coverage.xml", "")

        val arguments = ConvertCommand()
        arguments.inputFiles = listOf(execFile.absolutePath)
        arguments.outputFile = outputFile.absolutePath
        arguments.setClassDirectoriesOrZips(listOf(classFile.absolutePath))

        Converter(arguments).runJaCoCoReportGeneration()

        val xml = FileSystemUtils.readFileUTF8(outputFile)
        System.err.println(xml)
        assertThat(xml).isNotEmpty().contains("<class").contains("<counter").contains("TestClass")
    }

}
