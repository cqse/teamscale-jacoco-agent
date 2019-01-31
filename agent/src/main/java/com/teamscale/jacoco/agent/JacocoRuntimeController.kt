/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent

import com.teamscale.report.jacoco.dump.Dump
import org.jacoco.agent.rt.IAgent
import org.jacoco.agent.rt.RT
import org.jacoco.core.data.ExecutionDataReader
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.IExecutionDataVisitor
import org.jacoco.core.data.ISessionInfoVisitor
import org.jacoco.core.data.SessionInfo

import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Wrapper around JaCoCo's [RT] runtime interface.
 *
 *
 * Can be used if the calling code is run in the same JVM as the agent is
 * attached to.
 */
class JacocoRuntimeController
/** Constructor.  */
    (
    /** JaCoCo's [RT] agent instance  */
    private val agent: IAgent
) {

    /** Returns the current sessionId.  */
    /**
     * Sets the current sessionId of the agent that can be used to identify
     * which coverage is recorded from now on.
     */
    var sessionId: String
        get() = agent.sessionId
        set(sessionId) {
            agent.sessionId = sessionId
        }

    /** Indicates a failed dump.  */
    class DumpException
    /** Constructor.  */
        (message: String?, cause: Throwable) : Exception(message, cause) {
        companion object {

            /** Serialization ID.  */
            private const val serialVersionUID = 1L
        }

    }

    /**
     * Dumps execution data and resets it.
     *
     * @throws DumpException if dumping fails. This should never happen in real life. Dumping
     * should simply be retried later if this ever happens.
     */
    @Throws(JacocoRuntimeController.DumpException::class)
    fun dumpAndReset(): Dump {
        val binaryData = agent.getExecutionData(true)

        try {
            ByteArrayInputStream(binaryData).use { inputStream ->
                val reader = ExecutionDataReader(inputStream)

                val store = ExecutionDataStore()
                reader.setExecutionDataVisitor { store.put(it) }

                val sessionInfoVisitor = SessionInfoVisitor()
                reader.setSessionInfoVisitor(sessionInfoVisitor)

                reader.read()
                return Dump(sessionInfoVisitor.sessionInfo, store)
            }
        } catch (e: IOException) {
            throw DumpException("should never happen for the ByteArrayInputStream", e)
        }

    }

    /**
     * Dumps execution data to a file and resets it.
     *
     * @throws DumpException if dumping fails. This should never happen in real life. Dumping
     * should simply be retried later if this ever happens.
     */
    @Throws(JacocoRuntimeController.DumpException::class)
    fun dump() {
        try {
            agent.dump(true)
        } catch (e: IOException) {
            throw DumpException(e.message, e)
        }

    }

    /** Resets already collected coverage.  */
    fun reset() {
        agent.reset()
    }

    /**
     * Receives and stores a [SessionInfo]. Has a fallback dummy session in
     * case nothing is received.
     */
    private class SessionInfoVisitor : ISessionInfoVisitor {

        /** The received session info or a dummy.  */
        var sessionInfo = SessionInfo(
            "dummysession", System.currentTimeMillis(),
            System.currentTimeMillis()
        )

        /** {@inheritDoc}  */
        override fun visitSessionInfo(info: SessionInfo) {
            this.sessionInfo = info
        }

    }

}
