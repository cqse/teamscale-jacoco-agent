Feature: Calculator
  As a user
  I want to use a calculator to add numbers
  So that I don't need to add myself

  Scenario: Add two numbers
    Given I have a calculator
    When I add 0 and 0
    Then the result should be 0

  Scenario: Add two numbers
    Given I have a calculator
    When I add 1 and -1
    Then the result should be 0

  Scenario Outline: Add two numbers
    Given I have a calculator
    When I add <num1> and <num2>
    Then the result should be <total>

    Examples:
      | num1 | num2 | total |
      | -2   | 3    | 1     |
      | 10   | 15   | 25    |
      | 12   | 13   | 25    |


  Scenario Outline: Subtract two numbers <num1> & <num2>
    Given I have a calculator
    When I subtract <num2> from <num1>
    Then the result should be <difference>

    Examples:
      | num1 | num2 | difference |
      | 99   | 99   | 0          |
      | 10   | -1   | 11         |

  Scenario: Actually we just want to test a http://link
    Given I have a calculator
    When I add 0 and 1
    Then the result should be 1