package eu.cqse

import org.gradle.util.RelativePathUtil
import java.io.File

/**
 * Builder for javaagent parameters.
 * Produces something similar to option1=value1,option2=entry1:entry2:entry3,option3...
 */
class ArgumentAppender(private val builder: StringBuilder, private val workingDirectory: File) {

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
            appendKeyValue(name, RelativePathUtil.relativePath(workingDirectory, value))
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