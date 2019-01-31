/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact

import com.teamscale.jacoco.agent.AgentBase
import com.teamscale.jacoco.agent.AgentOptions
import com.teamscale.jacoco.agent.JacocoRuntimeController.DumpException
import spark.Request
import spark.Response

import spark.Spark.get
import spark.Spark.port
import spark.Spark.post
import spark.Spark.stop

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
class TestwiseCoverageAgent
/** Constructor.  */
@Throws(IllegalStateException::class)
constructor(
    /** The agent options.  */
    private val options: AgentOptions
) : AgentBase(options) {

    init {
        initServer()
    }

    /**
     * Starts the http server, which waits for information about started and finished tests.
     */
    private fun initServer() {
        logger.info("Listening for test events on port {}.", options.httpServerPort)
        port(options.httpServerPort!!)

        get("/test") { _, _ -> controller.sessionId }

        post("/test/start/$TEST_ID_PARAMETER") { request, _ ->
            handleTestStart(request)
            "success"
        }

        post("/test/end/$TEST_ID_PARAMETER") { request, _ ->
            handleTestEnd(request)
            "success"
        }
    }

    /** Handles the start of a new test case by setting the session ID.  */
    private fun handleTestStart(request: Request) {
        logger.debug("Start test " + request.params(TEST_ID_PARAMETER))

        // Dump and reset coverage so that we only record coverage that belongs to this particular test case.
        controller.reset()
        val testId = request.params(TestwiseCoverageAgent.TEST_ID_PARAMETER)
        controller.sessionId = testId
    }

    /** Handles the end of a test case by resetting the session ID.  */
    @Throws(DumpException::class)
    private fun handleTestEnd(request: Request) {
        logger.debug("End test " + request.params(TEST_ID_PARAMETER))
        controller.dump()
    }

    override fun prepareShutdown() {
        stop()
    }

    companion object {

        /** Path parameter placeholder used in the http requests.  */
        private val TEST_ID_PARAMETER = ":testId"
    }
}
