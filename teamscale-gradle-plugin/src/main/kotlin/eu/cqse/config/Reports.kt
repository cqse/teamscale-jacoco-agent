package eu.cqse.config

import org.gradle.api.Action
import java.io.Serializable

class Reports : Serializable {

    /** The partition for which artifacts are uploaded.  */
    fun setPartition(value: String) {
        testwiseCoverage.partition = testwiseCoverage.partition ?: value
        jUnit.partition = jUnit.partition ?: value
        jacoco.partition = jacoco.partition ?: value
    }

    /** The partition for which artifacts are uploaded.  */
    fun setMessage(value: String) {
        testwiseCoverage.message = testwiseCoverage.message ?: value
        jUnit.message = jUnit.message ?: value
        jacoco.message = jacoco.message ?: value
    }

    val testwiseCoverage = TestwiseCoverageConfiguration()
    val jUnit = JUnitReportConfiguration()
    val jacoco = JacocoReportConfiguration()
    val googleClosureCoverage = ClosureConfiguration()

    fun testwiseCoverage(action: Action<in TestwiseCoverageConfiguration>?) {
        testwiseCoverage.upload = true
        action?.execute(testwiseCoverage)
    }

    fun jUnit(action: Action<in JUnitReportConfiguration>?) {
        jUnit.upload = true
        action?.execute(jUnit)
    }

    fun jacoco(action: Action<in JacocoReportConfiguration>?) {
        jacoco.upload = true
        action?.execute(jacoco)
    }

    fun googleClosureCoverage(action: Action<in ClosureConfiguration>) {
        action.execute(googleClosureCoverage)
    }

    fun copyWithDefault(toCopy: Reports, default: Reports) {
        testwiseCoverage.copyWithDefault(toCopy.testwiseCoverage, default.testwiseCoverage)
        jUnit.copyWithDefault(toCopy.jUnit, default.jUnit)
        jacoco.copyWithDefault(toCopy.jacoco, default.jacoco)
        googleClosureCoverage.copyWithDefault(toCopy.googleClosureCoverage, default.googleClosureCoverage)
    }

    fun validate(): Boolean {
        return testwiseCoverage.partition != null
    }
}
