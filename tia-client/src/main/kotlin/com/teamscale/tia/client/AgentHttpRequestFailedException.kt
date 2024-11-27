package com.teamscale.tia.client

/**
 * Thrown if communicating with the agent via HTTP fails. The underlying reason can be either a network problem or an
 * internal error in the agent. Users of this library should report these exceptions appropriately so the underlying
 * problems can be addressed.
 */
class AgentHttpRequestFailedException : Exception {
	constructor(message: String?) : super(message)

	constructor(message: String?, cause: Throwable?) : super(message, cause)
}
