@file:JvmName(name = "PreMain")

package com.teamscale.jacoco.agent

import java.lang.instrument.Instrumentation
import java.net.URLClassLoader

/** Container class for the premain entry point for the agent.  */
object PreMain {

    /**
     * Entry point for the agent, called by the JVM.
     * The only purpose of this method is to enforce classloader isolation of
     * the agent from the rest of the application by running the actual agent
     * in a custom classloader that only has access to the agent's Jar file.
     *
     *
     * We do this by temporarily changing the thread's context classloader to
     * an [URLClassLoader] without a parent, starting the agent and then
     * resetting the context classloader.
     */
    @Throws(Exception::class)
    @JvmStatic
    fun premain(options: String?, instrumentation: Instrumentation) {
        val contextClassLoader = Thread.currentThread().contextClassLoader

        val agentJarUrl = PreMain::class.java.protectionDomain.codeSource.location
        val classloader = URLClassLoader(arrayOf(agentJarUrl), null)
        Thread.currentThread().contextClassLoader = classloader

        // we must use reflection to access the agent's methods because we don't share
        // the agent's classloader
        val agentName = PreMain::class.java.getPackage().name + ".AgentBase"
        val agentClass = classloader.loadClass(agentName)
        val method = agentClass.getMethod("premain", String::class.java, Instrumentation::class.java)
        method.invoke(null, options, instrumentation)

        Thread.currentThread().contextClassLoader = contextClassLoader
    }
}
