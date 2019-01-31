package com.teamscale.jacoco.agent

/**
 * Thrown if option parsing fails.
 */
class AgentOptionParseException : Exception {

    /**
     * Constructor.
     */
    constructor(message: String) : super(message) {}

    /**
     * Constructor.
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {}

    companion object {

        /**
         * Serialization ID.
         */
        private val serialVersionUID = 1L
    }

}
