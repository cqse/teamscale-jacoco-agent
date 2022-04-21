package com.teamscale.tia.maven;

import org.apache.maven.execution.MavenSession;

import java.util.Properties;
import java.util.function.Function;

public class ArgLineProperty {

	/**
	 * Name of the property used in the maven-osgi-test-plugin.
	 */
	public static final ArgLineProperty TYCHO_ARG_LINE = projectProperty("tycho.testArgLine");

	/**
	 * Name of the property used in the maven-surefire-plugin.
	 */
	public static final ArgLineProperty SUREFIRE_ARG_LINE = projectProperty("argLine");

	/**
	 * Name of the property used in the spring-boot-maven-plugin start goal.
	 */
	public static final ArgLineProperty SPRING_BOOT_ARG_LINE = userProperty("spring-boot.run.jvmArguments");

	public static final ArgLineProperty[] STANDARD_PROPERTIES = new ArgLineProperty[]{TYCHO_ARG_LINE, SUREFIRE_ARG_LINE, SPRING_BOOT_ARG_LINE};

	private static Properties getProjectProperties(MavenSession session) {
		return session.getCurrentProject().getProperties();
	}

	private static Properties getUserProperties(MavenSession session) {
		return session.getUserProperties();
	}

	public static ArgLineProperty projectProperty(String name) {
		return new ArgLineProperty(name, ArgLineProperty::getProjectProperties);
	}

	public static ArgLineProperty userProperty(String name) {
		return new ArgLineProperty(name, ArgLineProperty::getUserProperties);
	}

	public final String propertyName;
	private final Function<MavenSession, Properties> propertiesAccess;

	private ArgLineProperty(String propertyName,
							Function<MavenSession, Properties> propertiesAccess) {
		this.propertyName = propertyName;
		this.propertiesAccess = propertiesAccess;
	}

	public String getValue(MavenSession session) {
		return propertiesAccess.apply(session).getProperty(propertyName);
	}

	public void setValue(MavenSession session, String value) {
		propertiesAccess.apply(session).setProperty(propertyName, value);
	}
}
