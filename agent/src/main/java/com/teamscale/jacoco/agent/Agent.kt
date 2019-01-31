/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent

import com.teamscale.jacoco.agent.store.UploadStoreException
import com.teamscale.jacoco.agent.store.IXmlStore
import com.teamscale.jacoco.util.Benchmark
import com.teamscale.jacoco.util.Timer
import com.teamscale.report.jacoco.JaCoCoXmlReportGenerator
import com.teamscale.report.jacoco.dump.Dump

import java.io.IOException
import java.time.Duration

import com.teamscale.jacoco.util.LoggingUtils.wrap

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and
 * XML conversion based on a time interval.
 */
class Agent
/** Constructor.  */
/*package*/ @Throws(IllegalStateException::class, UploadStoreException::class)
internal constructor(options: AgentOptions) : AgentBase(options) {

    /** Converts binary data to XML.  */
    private val generator: JaCoCoXmlReportGenerator

    /** Regular dump task.  */
    private var timer: Timer? = null

    /** Stores the XML files.  */
    protected val store: IXmlStore

    init {

        store = options.createStore()
        logger.info("Storage method: {}", store.describe())

        generator = JaCoCoXmlReportGenerator(
            options.classDirectoriesOrZips,
            options.locationIncludeFilter,
            options.shouldIgnoreDuplicateClassFiles(), wrap(logger)
        )

        if (options.shouldDumpInIntervals()) {
            timer = Timer(
                Runnable { this.dumpReport() },
                options.dumpIntervalInMinutes
            )
            timer!!.start()
            logger.info("Dumping every {} minutes.", options.dumpIntervalInMinutes)
        }
    }

    override fun prepareShutdown() {
        if (timer != null) {
            timer!!.stop()
        }
        dumpReport()
    }

    /**
     * Dumps the current execution data, converts it and writes it to the
     * [.store]. Logs any errors, never throws an exception.
     */
    private fun dumpReport() {
        logger.debug("Starting dump")

        try {
            dumpReportUnsafe()
        } catch (t: Throwable) {
            // we want to catch anything in order to avoid crashing the whole system under test
            logger.error("Dump job failed with an exception", t)
        }

    }

    private fun dumpReportUnsafe() {
        val dump: Dump
        try {
            dump = controller.dumpAndReset()
        } catch (e: JacocoRuntimeController.DumpException) {
            logger.error("Dumping failed, retrying later", e)
            return
        }

        var xml = ""
        try {
            Benchmark("Generating the XML report").use { xml = generator.convert(dump) }
        } catch (e: IOException) {
            logger.error("Converting binary dump to XML failed", e)
            return
        }

        store.store(xml)
    }
}
