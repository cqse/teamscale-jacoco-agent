package com.teamscale.client

/**
 * Contains details about a test.
 */
class TestDetails
/** Constructor.  */
    (
    /** Unique name of the test case by using a path like hierarchical description, which can be shown in the UI.  */
    val uniformPath: String,
    /**
     * Path to the source of the method. Will be equal to uniformPath in most cases, but e.g. @Test methods in a Base
     * class will have the sourcePath pointing to the Base class which contains the actual implementation whereas
     * uniformPath will contain the the class name of the most specific subclass, from where it was actually executed.
     */
    val sourcePath: String?,
    /**
     * Some kind of content to tell whether the test specification has changed. Can be revision number or
     * hash over the specification or similar.
     */
    val content: String?
)