package eu.cqse.config

import org.gradle.api.Action
import java.io.Serializable

class Reports : Serializable {

    val testwiseCoverage = TestwiseCoverageConfiguration()
    val jUnit = JUnitReportConfiguration()
    val googleClosureCoverage = ClosureConfiguration()

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

    fun testwiseCoverage(action: Action<in TestwiseCoverageConfiguration>?) {
        testwiseCoverage.upload = true
        action?.execute(testwiseCoverage)
    }

    fun jUnit(action: Action<in JUnitReportConfiguration>?) {
        jUnit.upload = true
        action?.execute(jUnit)
    }

    fun googleClosureCoverage(action: Action<in ClosureConfiguration>) {
        action.execute(googleClosureCoverage)
    }

    fun copyWithDefault(toCopy: Reports, default: Reports) {
        testwiseCoverage.copyWithDefault(toCopy.testwiseCoverage, default.testwiseCoverage)
        jUnit.copyWithDefault(toCopy.jUnit, default.jUnit)
        googleClosureCoverage.copyWithDefault(toCopy.googleClosureCoverage, default.googleClosureCoverage)
    }

    fun validate(): Boolean {
        return testwiseCoverage.partition != null
    }
}
