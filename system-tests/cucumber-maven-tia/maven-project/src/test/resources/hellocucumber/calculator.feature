Feature: Calculator
  As a user
  I want to use a calculator to add numbers
  So that I don't need to add myself

  Scenario: Add two numbers 0 & 0
    Given I have a calculator
    When I add 0 and 0
    Then the result should be 0

  Scenario Outline: Add two numbers <num1> & <num2>
    Given I have a calculator
    When I add <num1> and <num2>
    Then the result should be <total>

    Examples:
      | num1 | num2 | total |
      | -2   | 3    | 1     |
      | 10   | 15   | 25    |
      | 1    | -1   | 0     |

  Scenario Outline: Add two numbers <num1> & <num2>
    Given I have a calculator
    When I add <num1> and <num2>
    Then the result should be <total>

    Examples:
      | num1 | num2 | total |
      | 99   | -99  | 0     |
      | -1   | -10  | -11   |
