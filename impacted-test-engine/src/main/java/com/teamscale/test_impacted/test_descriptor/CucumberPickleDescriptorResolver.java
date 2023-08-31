package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.logging.Logger;

public class CucumberPickleDescriptorResolver implements ITestDescriptorResolver {
	public static final String CUCUMBER_ENGINE_ID = "cucumber";
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

		return Optional.of(featurePath.get() + "/" + pickleName.get());
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
		// PickleDescriptor is not public, so we can't import and use it to get access to the pickle attribute containing the name => reflection
		// https://github.com/cucumber/cucumber-jvm/blob/main/cucumber-junit-platform-engine/src/main/java/io/cucumber/junit/platform/engine/NodeDescriptor.java#L96
		// We want to use the name, though, because all other fields of the test descriptor like the unique id and the (legacy-) display name can easily result in inconsistencies, e.g. for
		// Scenario Outline: Add two numbers <num1> & <num2>
		//    Given I have a calculator
		//    When I add <num1> and <num2>
		//    Then the result should be <total>
		//
		//    Examples:
		//      | num1 | num2 | total |
		//      | -2   | 3    | 1     |
		//      | 10   | 15   | 25    |
		//      | 12   | 13   | 25    |
		// tests will be executed for every line of the examples table. The unique id refers to the line number (!) of the example in the .feature file
		// and the (legacy-) display name on the index of the example in the table. So for the first example, we'll have
		// unique id: [...][feature:classpath%3Ahellocucumber%2Fcalculator.feature]/[scenario:11]/[examples:16]/[example:18] <- the latter numbers are line numbers in the file!!
		// (legacy-) display name: Example #1.1 <- example index in the file (changes if you add another tests above)
		// testDescritor.pickle.pickle.name: Add two numbers -2 & 3 <- should be unique in the vast majority of times
		// => So the pickle name is the most stable and meaningful one
		try {
			Field pickleField = testDescriptor.getClass().getDeclaredField("pickle");
			pickleField.setAccessible(true);
			Object pickle = pickleField.get(testDescriptor);
			Field internalPickleField = pickle.getClass().getDeclaredField("pickle");
			internalPickleField.setAccessible(true);
			Object internalPickle = internalPickleField.get(pickle);
			Field nameField = internalPickle.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			return Optional.ofNullable(nameField.get(internalPickle).toString());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			return Optional.empty();
		}
	}
}
