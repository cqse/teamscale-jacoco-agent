package com.teamscale.jacoco.agent.store

/** Stores XML data permanently.  */
interface IXmlStore {

    /** Stores the given XML permanently.  */
    fun store(xml: String)

    /** Human-readable description of the store.  */
    fun describe(): String

}
