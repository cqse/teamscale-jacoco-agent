package hellocucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.*;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assertions.*;
import org.junit.matchers.JUnitMatchers;

public class StepDefinitions {
    private int total;

    private Calculator calculator;

    @Before
    private void init() {
        total = -999;
    }

    @Given("^I have a calculator$")
    public void initializeCalculator() throws Throwable {
        calculator = new Calculator();
    }

    @When("^I add (-?\\d+) and (-?\\d+)$")
    public void testAdd(int num1, int num2) throws Throwable {
        total = calculator.add(num1, num2);
    }

    @Then("^the result should be (-?\\d+)$")
    public void validateResult(int result) throws Throwable {
        Assert.assertThat(total, Matchers.equalTo(result));
    }
}
