package com.teamscale.maven.tia;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.util.Properties;
import java.util.function.Function;

/** Accessor for different types of properties, e.g. project or user properties, in a {@link MavenSession}. */
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

	/** The standard properties that this plugin might modify. */
	public static final ArgLineProperty[] STANDARD_PROPERTIES = new ArgLineProperty[]{TYCHO_ARG_LINE, SUREFIRE_ARG_LINE, SPRING_BOOT_ARG_LINE};

	private static Properties getProjectProperties(MavenSession session) {
		return session.getCurrentProject().getProperties();
	}

	private static Properties getUserProperties(MavenSession session) {
		return session.getUserProperties();
	}

	/** Creates a project property ({@link MavenProject#getProperties()}). */
	public static ArgLineProperty projectProperty(String name) {
		return new ArgLineProperty(name, ArgLineProperty::getProjectProperties);
	}

	/** Creates a user property ({@link MavenSession#getUserProperties()}). */
	public static ArgLineProperty userProperty(String name) {
		return new ArgLineProperty(name, ArgLineProperty::getUserProperties);
	}

	/** The name of the property. */
	public final String propertyName;
	private final Function<MavenSession, Properties> propertiesAccess;

	private ArgLineProperty(String propertyName,
							Function<MavenSession, Properties> propertiesAccess) {
		this.propertyName = propertyName;
		this.propertiesAccess = propertiesAccess;
	}

	/** Returns the value of this property in the given Maven session */
	public String getValue(MavenSession session) {
		return propertiesAccess.apply(session).getProperty(propertyName);
	}

	/** Sets the value of this property in the given Maven session */
	public void setValue(MavenSession session, String value) {
		propertiesAccess.apply(session).setProperty(propertyName, value);
	}
}
