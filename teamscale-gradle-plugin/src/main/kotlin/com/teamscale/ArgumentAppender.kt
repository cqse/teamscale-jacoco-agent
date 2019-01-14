package com.teamscale

import java.io.File

/**
 * Builder for java agent parameters.
 * Produces something similar to option1=value1,option2=entry1:entry2:entry3,option3...
 */
class ArgumentAppender(private val builder: StringBuilder) {

    /** Whether there have already been arguments appended. */
    private var anyArgs: Boolean = false

    /** Appends the given key value pair as an argument for the javaagent. */
    fun append(name: String, value: Any?) {
        if (value == null) {
            return
        }

        if (value is Collection<*>) {
            if (!value.isEmpty()) {
                appendKeyValue(name, value.joinToString(":"))
            }
        } else if (value is File) {
            appendKeyValue(name, value.canonicalPath)
        } else {
            appendKeyValue(name, value.toString())
        }

    }

    private fun appendKeyValue(key: String, value: String) {
        if (anyArgs) {
            builder.append(",")
        }

        builder.append(key).append("=").append(value)
        anyArgs = true
    }
}