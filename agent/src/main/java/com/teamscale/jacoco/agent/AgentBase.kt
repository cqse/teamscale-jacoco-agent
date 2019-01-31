package com.teamscale.jacoco.agent

import com.teamscale.jacoco.util.LoggingUtils
import org.jacoco.agent.rt.RT
import java.lang.instrument.Instrumentation

/**
 * Base class for agent implementations. Handles logger shutdown,
 * store creation and instantiation of the [JacocoRuntimeController].
 *
 *
 * Subclasses must handle dumping into the store.
 */
abstract class AgentBase
/** Constructor.  */
@Throws(IllegalStateException::class)
constructor(options: AgentOptions) {

    /** The logger.  */
    protected val logger = LoggingUtils.getLogger(this)

    /** Controls the JaCoCo runtime.  */
    protected val controller: JacocoRuntimeController

    init {
        try {
            controller = JacocoRuntimeController(RT.getAgent())
        } catch (e: IllegalStateException) {
            throw IllegalStateException(
                "JaCoCo agent not started or there is a conflict with another JaCoCo agent on the classpath.", e
            )
        }

        logger.info("Starting JaCoCo agent with options: {}", options.originalOptionsString)
    }

    /**
     * Registers a shutdown hook that stops the timer and dumps coverage a final
     * time.
     */
    private fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            prepareShutdown()
            logger.info("CQSE JaCoCo agent successfully shut down.")
            loggingResources!!.close()
        })
    }

    /** Called when the shutdown hook is triggered.  */
    protected abstract fun prepareShutdown()

    companion object {

        private var loggingResources: LoggingUtils.LoggingResources? = null

        /** Called by the actual premain method once the agent is isolated from the rest of the application.  */
        @Throws(Exception::class)
        @JvmStatic
        fun premain(options: String, instrumentation: Instrumentation) {
            val agentOptions: AgentOptions
            val delayedLogger = DelayedLogger()
            try {
                agentOptions = AgentOptionsParser.parse(options, delayedLogger)
            } catch (e: AgentOptionParseException) {
                LoggingUtils.initializeDefaultLogging().use {
                    val logger = LoggingUtils.getLogger(AgentBase::class.java)
                    delayedLogger.logTo(logger)
                    logger.error("Failed to parse agent options: " + e.message, e)
                    System.err.println("Failed to parse agent options: " + e.message)
                    throw e
                }
            }

            loggingResources = LoggingUtils.initializeLogging(agentOptions.loggingConfig)

            val logger = LoggingUtils.getLogger(Agent::class.java)
            delayedLogger.logTo(logger)
            logger.info("Starting JaCoCo's agent")
            org.jacoco.agent.rt.internal_28bab1d.PreMain.premain(
                agentOptions.createJacocoAgentOptions(),
                instrumentation
            )

            val agent = agentOptions.createAgent()
            agent.registerShutdownHook()
        }
    }
}
