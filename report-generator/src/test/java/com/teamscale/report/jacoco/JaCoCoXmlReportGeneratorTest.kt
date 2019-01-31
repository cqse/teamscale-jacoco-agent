package com.teamscale.report.jacoco

import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.util.AntPatternIncludeFilter
import com.teamscale.report.util.ILogger
import org.conqat.lib.commons.collections.CollectionUtils
import org.conqat.lib.commons.test.CCSMTestCaseBase
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfo
import org.junit.Test

import java.io.File
import java.io.IOException
import java.util.Collections

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.Mockito.mock

/** Tests report generation with and without duplicate classes.  */
class JaCoCoXmlReportGeneratorTest : CCSMTestCaseBase() {

    /** Ensures that the normal case runs without exceptions.  */
    @Test
    @Throws(Exception::class)
    fun testNormalCaseThrowsNoException() {
        runGenerator("no-duplicates", false)
    }

    /** Ensures that two identical duplicate classes do not cause problems.  */
    @Test
    @Throws(Exception::class)
    fun testIdenticalClassesShouldNotThrowException() {
        runGenerator("identical-duplicate-classes", false)
    }

    /**
     * Ensures that two non-identical, duplicate classes cause an exception to be
     * thrown.
     */
    @Test
    @Throws(Exception::class)
    fun testDifferentClassesWithTheSameNameShouldThrowException() {
        assertThatThrownBy { runGenerator("different-duplicate-classes", false) }
            .isExactlyInstanceOf(IOException::class.java).hasCauseExactlyInstanceOf(IllegalStateException::class.java)
    }

    /**
     * Ensures that two non-identical, duplicate classes do not cause an exception
     * to be thrown if the ignore-duplicates flag is set.
     */
    @Test
    @Throws(Exception::class)
    fun testDifferentClassesWithTheSameNameShouldNotThrowExceptionIfFlagIsSet() {
        runGenerator("different-duplicate-classes", true)
    }

    /** Creates a dummy dump.  */
    private fun createDummyDump(): Dump {
        val store = ExecutionDataStore()
        store.put(ExecutionData(123, "TestClass", booleanArrayOf(true, true, true)))
        val info = SessionInfo("session-id", 124L, 125L)
        return Dump(info, store)
    }

    /** Runs the report generator.  */
    @Throws(IOException::class)
    private fun runGenerator(testDataFolder: String, shouldIgnoreDuplicates: Boolean) {
        val classFileFolder = useTestFile(testDataFolder)
        val includeFilter = AntPatternIncludeFilter(
            CollectionUtils.emptyList<Any>(),
            CollectionUtils.emptyList<Any>()
        )
        JaCoCoXmlReportGenerator(
            listOf(classFileFolder), includeFilter, shouldIgnoreDuplicates,
            mock(ILogger::class.java)
        ).convert(createDummyDump())
    }

}
