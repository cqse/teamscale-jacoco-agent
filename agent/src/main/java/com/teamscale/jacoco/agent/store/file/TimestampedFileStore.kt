package com.teamscale.jacoco.agent.store.file

import com.teamscale.client.EReportFormat
import com.teamscale.jacoco.agent.store.IXmlStore
import com.teamscale.jacoco.util.Benchmark
import com.teamscale.jacoco.util.LoggingUtils
import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.slf4j.Logger

import java.io.IOException
import java.nio.file.Path

/**
 * Writes XMLs to files in a folder. The files are timestamped with the time of
 * writing the trace to make each file reasonably unique so they don't overwrite
 * each other.
 */
class TimestampedFileStore
/** Constructor.  */
    (
    /** The directory to which to write the XML files.  */
    /** @see .outputDirectory
     */
    val outputDirectory: Path
) : IXmlStore {

    /** The logger.  */
    private val logger = LoggingUtils.getLogger(this)

    /** {@inheritDoc}  */
    override fun store(xml: String) {
        Benchmark("Writing the JaCoCo report to a file").use {
            val currentTime = System.currentTimeMillis()
            val outputPath = outputDirectory.resolve("jacoco-$currentTime.xml")
            try {
                FileSystemUtils.writeFile(outputPath.toFile(), xml)
            } catch (e: IOException) {
                logger.error("Failed to write XML to {}", outputPath, e)
            }
        }
    }

    /** {@inheritDoc}  */
    override fun describe(): String {
        return "Saving to local filesystem path $outputDirectory"
    }

}
