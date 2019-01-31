/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco.dump

import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfo

/** All data received in one dump.  */
class Dump
/** Constructor.  */
    (
    /** The session info.  */
    val info: SessionInfo,
    /** The execution data store.  */
    val store: ExecutionDataStore
)
