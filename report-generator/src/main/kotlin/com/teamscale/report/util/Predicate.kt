package com.teamscale.report.util

/**
 * Represents a predicate (boolean-valued function) of one argument.
 *
 * @param <T> the type of the input to the predicate
</T> */
interface Predicate<T> {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t the input argument
     * @return `true` if the input argument matches the predicate,
     * otherwise `false`
     */
    fun test(t: T): Boolean

}
