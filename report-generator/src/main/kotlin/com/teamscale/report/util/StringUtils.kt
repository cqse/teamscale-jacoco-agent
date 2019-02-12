package com.teamscale.report.util

val CR: String = System.getProperty("line.separator")


/**
 * Calculates the edit distance (aka Levenshtein distance) for two strings, i.e.
 * the number of insert, delete or replace operations required to transform one
 * string into the other. The running time is O(n*m) and the space complexity is
 * O(n+m), where n/m are the lengths of the strings. Note that due to the high
 * running time, for long strings the [Diff] class should be used, that
 * has a more efficient algorithm, but only for insert/delete (not replace
 * operation).
 *
 * Although this is a clean reimplementation, the basic algorithm is explained
 * here: http://en.wikipedia.org/wiki/Levenshtein_distance#
 * Iterative_with_two_matrix_rows
 */
fun String.editDistance(t: String): Int {
    val s: String = this
    val sChars = s.toCharArray()
    val tChars = t.toCharArray()
    val m = s.length
    val n = t.length

    var distance = IntArray(m + 1)
    for (i in 0..m) {
        distance[i] = i
    }

    var oldDistance = IntArray(m + 1)
    for (j in 1..n) {

        // swap distance and oldDistance
        val tmp = oldDistance
        oldDistance = distance
        distance = tmp

        distance[0] = j
        for (i in 1..m) {
            var cost = 1 + Math.min(distance[i - 1], oldDistance[i])
            if (sChars[i - 1] == tChars[j - 1]) {
                cost = Math.min(cost, oldDistance[i - 1])
            } else {
                cost = Math.min(cost, 1 + oldDistance[i - 1])
            }
            distance[i] = cost
        }
    }

    return distance[m]
}