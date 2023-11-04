package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Test descriptor resolver for Cucumber. For details how we extract the uniform path, see comment in
 * {@link #getPickleName(TestDescriptor)}. The cluster id is the .feature file in which the tests are defined.
 */
public class CucumberPickleDescriptorResolver implements ITestDescriptorResolver {
	/** Name of the cucumber test engine as used in the unique id of the test descriptor */
	public static final String CUCUMBER_ENGINE_ID = "cucumber";
	/** Type of the unique id segment of a test descriptor representing a cucumber feature file */
	public static final String FEATURE_SEGMENT_TYPE = "feature";

	private static final Logger LOGGER = LoggerUtils.getLogger(CucumberPickleDescriptorResolver.class);

	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		Optional<String> featurePath = getFeaturePath(testDescriptor);
		if (!featurePath.isPresent()) {
			LOGGER.severe(() -> "Cannot resolve the feature classpath for " +
					testDescriptor + ". This is probably a bug. Please report to CQSE");
			return Optional.empty();
		}
		Optional<String> pickleName = getPickleName(testDescriptor);
		if (!pickleName.isPresent()) {
			LOGGER.severe(() -> "Cannot resolve the pickle name for " +
					testDescriptor + ". This is probably a bug. Please report to CQSE");
			return Optional.empty();
		}
		String uniformPath = featurePath.get() + "/" + pickleName.get();

		// Add an index to the end of the name in case multiple tests have the same name in the same feature file
		Optional<TestDescriptor> featureFileTestDescriptor = getFeatureFileTestDescriptor(testDescriptor);
		if (featureFileTestDescriptor.isPresent()) {
			List<? extends TestDescriptor> siblingTestsWithTheSameName = flatListOfAllTestDescriptorChildrenWithPickleName(
					featureFileTestDescriptor.get(), pickleName.get());
			int indexOfCurrentTest = siblingTestsWithTheSameName.indexOf(testDescriptor) + 1;
			uniformPath += " #" + indexOfCurrentTest;
		}

		return Optional.of(uniformPath);
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		return getFeaturePath(testDescriptor);
	}

	@Override
	public String getEngineId() {
		return CUCUMBER_ENGINE_ID;
	}

	/**
	 * Transform unique id segments from something like
	 * [feature:classpath%3Ahellocucumber%2Fcalculator.feature]/[scenario:11]/[examples:16]/[example:21] to
	 * hellocucumber/calculator.feature/11/16/21
	 */
	private Optional<String> getFeaturePath(TestDescriptor testDescriptor) {
		Optional<String> featureClasspath = TestDescriptorUtils.getUniqueIdSegment(testDescriptor,
				FEATURE_SEGMENT_TYPE);
		return featureClasspath.map(featureClasspathString -> featureClasspathString.replaceAll("classpath:", ""));
	}

	private Optional<String> getPickleName(TestDescriptor testDescriptor) {
		// The PickleDescriptor test descriptor class is not public, so we can't import and use it to get access to the pickle attribute containing the name => reflection
		// https://github.com/cucumber/cucumber-jvm/blob/main/cucumber-junit-platform-engine/src/main/java/io/cucumber/junit/platform/engine/NodeDescriptor.java#L90
		// We want to use the name, though, because the unique id of the test descriptor can easily result in inconsistencies,
		// e.g. for
		//
		// Scenario Outline: Add two numbers
		//    Given I have a calculator
		//    When I add <num1> and <num2>
		//    Then the result should be <total>
		//
		//    Examples:
		//      | num1 | num2 | total |
		//      | -2   | 3    | 1     |
		//      | 10   | 15   | 25    |
		//      | 12   | 13   | 25    |
		//
		// tests will be executed for every line of the examples table. The unique id refers to the line number (!) of the example in the .feature file.
		// unique id: [...][feature:classpath%3Ahellocucumber%2Fcalculator.feature]/[scenario:11]/[examples:16]/[example:18] <- the latter numbers are line numbers in the file!!
		// This means, everytime the line numbers change the test would not be recognised as the same in Teamscale anymore.
		// So we use the pickle name (testDescriptor.pickle.getName()) to get the descriptive name "Add two numbers".
		// This is not unique yet, as all the executions of the test (all examples) will have the same name then => may not be the case in Teamscale.
		// To resolve this, we add an index afterwards in getUniformPath()

		Field pickleField = null;
		try {
			pickleField = testDescriptor.getClass().getDeclaredField("pickle");
		} catch (NoSuchFieldException e) {
			// Pre cucumber 7.11.2, the field was called pickleEvent (see NodeDescriptor in this merge request: https://github.com/cucumber/cucumber-jvm/pull/2711/files)
			// ...
		}
		try {
			if (pickleField == null) {
				// ... so try again with "pickleEvent"
				pickleField = testDescriptor.getClass().getDeclaredField("pickleEvent");
			}
			pickleField.setAccessible(true);
			Object pickle = pickleField.get(testDescriptor);
			// getName() is required by the pickle interface https://github.com/cucumber/cucumber-jvm/blob/main/cucumber-gherkin/src/main/java/io/cucumber/core/gherkin/Pickle.java#L14
			Method getNameMethod = pickle.getClass().getDeclaredMethod("getName");
			getNameMethod.setAccessible(true);
			String name = getNameMethod.invoke(pickle).toString();
			return Optional.of(name);
		} catch (NoSuchFieldException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
			return Optional.empty();
		}
	}

	private Optional<TestDescriptor> getFeatureFileTestDescriptor(TestDescriptor testDescriptor) {
		if (!isFeatureFileTestDescriptor(testDescriptor)) {
			if (!testDescriptor.getParent().isPresent()) {
				return Optional.empty();
			}
			return getFeatureFileTestDescriptor(testDescriptor.getParent().get());
		}
		return Optional.of(testDescriptor);
	}

	private boolean isFeatureFileTestDescriptor(TestDescriptor cucumberTestDescriptor) {
		return cucumberTestDescriptor.getUniqueId().getLastSegment().getType().equals(FEATURE_SEGMENT_TYPE);
	}

	private List<TestDescriptor> flatListOfAllTestDescriptorChildrenWithPickleName(TestDescriptor testDescriptor,
																				   String pickleName) {
		if (testDescriptor.getChildren().isEmpty()) {
			Optional<String> pickleId = getPickleName(testDescriptor);
			if (pickleId.isPresent() && pickleName.equals(pickleId.get())) {
				return Collections.singletonList(testDescriptor);
			}
			return Collections.emptyList();
		}
		List<TestDescriptor> flattenedChildDescriptors = new ArrayList<>();
		for (TestDescriptor childDescriptor : testDescriptor.getChildren()) {
			flattenedChildDescriptors.addAll(
					flatListOfAllTestDescriptorChildrenWithPickleName(childDescriptor, pickleName));
		}
		return flattenedChildDescriptors;

	}
}
