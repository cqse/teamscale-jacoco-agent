package eu.cqse.config

import org.gradle.api.Action
import java.io.Serializable

/** Holds all the configuration of all report types that should be generated and uploaded to Teamscale. */
class Reports : Serializable {

    /** The testwise coverage configuration. */
    val testwiseCoverage = TestwiseCoverageConfiguration()

    /** The jUnit report configuration. */
    val jUnit = JUnitReportConfiguration()

    /** The google closure coverage configuration. */
    val googleClosureCoverage = GoogleClosureConfiguration()

    /** The partition for which artifacts are uploaded. This sets the partition for all reports. */
    fun setPartition(value: String) {
        testwiseCoverage.partition = testwiseCoverage.partition ?: value
        jUnit.partition = jUnit.partition ?: value
    }

    /** The partition for which artifacts are uploaded. This sets the message for all reports. */
    fun setMessage(value: String) {
        testwiseCoverage.message = testwiseCoverage.message ?: value
        jUnit.message = jUnit.message ?: value
    }

    /** Specifies that testwise coverage should be generated and uploaded. */
    fun testwiseCoverage(action: Action<in TestwiseCoverageConfiguration>?) {
        testwiseCoverage.upload = true
        action?.execute(testwiseCoverage)
    }

    /** Specifies that a junit report should be generated and uploaded. */
    fun jUnit(action: Action<in JUnitReportConfiguration>?) {
        jUnit.upload = true
        action?.execute(jUnit)
    }

    /** Specifies that google closure coverage should be uploaded as testwise coverage. */
    fun googleClosureCoverage(action: Action<in GoogleClosureConfiguration>) {
        action.execute(googleClosureCoverage)
    }

    /** Creates a copy of the reports object, setting all non-set values to their default value. */
    fun copyWithDefault(toCopy: Reports, default: Reports) {
        testwiseCoverage.copyWithDefault(toCopy.testwiseCoverage, default.testwiseCoverage)
        jUnit.copyWithDefault(toCopy.jUnit, default.jUnit)
        googleClosureCoverage.copyWithDefault(toCopy.googleClosureCoverage, default.googleClosureCoverage)
    }
}
