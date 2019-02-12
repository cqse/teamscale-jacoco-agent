/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2017 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.commandline

import com.teamscale.report.util.CR
import java.util.*

/**
 * Helper class to allow for multiple validations to occur.
 */
class Validator {

    /** The found validation problems in the form of error messages for the user.  */
    private val messages = ArrayList<String>()

    /** Returns `true` if the validation succeeded.  */
    val isValid: Boolean
        get() = messages.isEmpty()

    /** Returns an error message with all validation problems that were found.  */
    val errorMessage: String
        get() = "- " + messages.joinToString("$CR- ")

    /** Runs the given validation routine.  */
    fun ensure(validation: () -> Unit) {
        try {
            validation()
        } catch (e: Exception) {
            messages.add("${e.message}")
        } catch (e: IllegalArgumentException) {
            messages.add("${e.message}")
        }
    }

    /**
     * Checks that the given condition is `true` or adds the given error
     * message.
     */
    fun isTrue(condition: Boolean, message: String) {
        ensure { require(condition) { message } }
    }

    /**
     * Checks that the given condition is `false` or adds the given error
     * message.
     */
    fun isFalse(condition: Boolean, message: String) {
        ensure { require(!condition) { message } }
    }

}
