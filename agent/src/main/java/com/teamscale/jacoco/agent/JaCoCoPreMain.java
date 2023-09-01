/*******************************************************************************
 * Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/

package com.teamscale.jacoco.agent;

import org.jacoco.agent.rt.internal_b6258fc.Agent;
import org.jacoco.agent.rt.internal_b6258fc.CoverageTransformer;
import org.jacoco.agent.rt.internal_b6258fc.IExceptionLogger;
import org.jacoco.agent.rt.internal_b6258fc.core.runtime.AgentOptions;
import org.jacoco.agent.rt.internal_b6258fc.core.runtime.IRuntime;
import org.jacoco.agent.rt.internal_b6258fc.core.runtime.InjectedClassRuntime;
import org.jacoco.agent.rt.internal_b6258fc.core.runtime.ModifiedSystemClassRuntime;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This is a copy of the PreMain class from the JaCoCo agent. The only changes are that we:
 * <ul>
 * <li>replaced the {@link CoverageTransformer} with our {@link LenientCoverageTransformer}</li>
 * <li>pass a {@link Logger} to {@link #premain(String, Instrumentation, Logger)} which is passed to the
 * {@link LenientCoverageTransformer} instead of {@link IExceptionLogger}</li>
 * </ul>
 * <p>
 * The agent which is referred as the <code>Premain-Class</code>. The agent configuration is provided with the agent
 * parameters in the command line.
 */
public final class JaCoCoPreMain {

	private JaCoCoPreMain() {
		// no instances
	}

	/**
	 * This method is called by the JVM to initialize Java agents.
	 *
	 * @param options agent options
	 * @param inst    instrumentation callback provided by the JVM
	 * @throws Exception in case initialization fails
	 */
	public static void premain(final String options, final Instrumentation inst, Logger logger)
			throws Exception {

		final AgentOptions agentOptions = new AgentOptions(options);

		final Agent agent = Agent.getInstance(agentOptions);

		final IRuntime runtime = createRuntime(inst);
		runtime.startup(agent.getData());
		inst.addTransformer(new LenientCoverageTransformer(runtime, agentOptions,
				logger));
	}

	private static IRuntime createRuntime(final Instrumentation inst)
			throws Exception {

		if (redefineJavaBaseModule(inst)) {
			return new InjectedClassRuntime(Object.class, "$JaCoCo");
		}

		return ModifiedSystemClassRuntime.createFor(inst,
				"java/lang/UnknownError");
	}

	/**
	 * Opens {@code java.base} module for {@link InjectedClassRuntime} when executed on Java 9 JREs or higher.
	 *
	 * @return <code>true</code> when running on Java 9 or higher,
	 * <code>false</code> otherwise
	 * @throws Exception if unable to open
	 */
	private static boolean redefineJavaBaseModule(
			final Instrumentation instrumentation) throws Exception {
		try {
			Class.forName("java.lang.Module");
		} catch (final ClassNotFoundException e) {
			return false;
		}

		Instrumentation.class.getMethod("redefineModule", //
				Class.forName("java.lang.Module"), //
				Set.class, //
				Map.class, //
				Map.class, //
				Set.class, //
				Map.class //
		).invoke(instrumentation, // instance
				getModule(Object.class), // module
				Collections.emptySet(), // extraReads
				Collections.emptyMap(), // extraExports
				Collections.singletonMap("java.lang",
						Collections.singleton(
								getModule(InjectedClassRuntime.class))), // extraOpens
				Collections.emptySet(), // extraUses
				Collections.emptyMap() // extraProvides
		);
		return true;
	}

	/**
	 * @return {@code cls.getModule()}
	 */
	private static Object getModule(final Class<?> cls) throws Exception {
		return Class.class //
				.getMethod("getModule") //
				.invoke(cls);
	}

}
