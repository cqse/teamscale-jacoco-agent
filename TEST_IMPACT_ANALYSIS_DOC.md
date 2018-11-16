# Test Impact Analysis in Teamscale

## Concept

Regression testing is typically done in a retest-all strategy, even if most of the tests are
irrelevant when only considering a given small change. By collecting data of which test executes
which part of the system we can find out which tests are actually impacted by a given change.
More concrete this means we have to gather coverage information per test case.

Teamscale with its Test Impact Analysis augments this data with source code change
information and calculates the impacted tests for one or more changes. It's then the
responsibility of the test framework to execute only those.

## Setup

## Technical details

### 1. Upload Test Details and query impacted tests

Since Teamscale does not have knowledge beforehand of which tests are available for execution, the list of available 
tests has to be uploaded to Teamscale.

```json
[
  {
    "uniformPath": "com/example/JUnit5Test/myFirstTest",
    "displayName": "My 1st JUnit 5 test! 😎"
  },
  {
    "uniformPath": "com/example/JUnit4Test/testAdd",
    "displayName": "testAdd"
  },
  {
    "uniformPath": "com/example/JUnit4Test/systemTest",
    "displayName": "systemTest"
  }
]
```

- `uniformPath` Is a file system like path, which is used to uniquely identify a test within Teamscale and should be 
  chosen accordingly. It is furthermore used to make the set of tests hierarchically navigable within Teamscale.
- `displayName` *(optional)* Is a human readable name of the test itself used for UI only.
- `sourcePath` *(optional)* Path to the source of the method if the test is specified in a programming language and is 
  known to Teamscale. Will be equal to `uniformPath` in most cases, but e.g. in JUnit `@Test` annotated methods in a base 
  class will have the sourcePath pointing to the base class, which contains the actual implementation, whereas 
  `uniformPath` will contain the class name of the most specific subclass from where it was actually executed.
- `content` *(optional)* Identifies the content of the test specification. This can be used to indicate that the 
  specification of a test has changed and therefore should be re-executed. The value can be an arbitrary string value 
  e.g. a hash of the test specification or a revision number of the test.

The upload is possible via the `Report Upload` REST-API with the `TEST_LIST` format.

//TODO 

You can get the impacted tests from Teamscale via the `Test Impact` REST-API, which returns a list of external test IDs 
that can be used to select the according tests for execution.
Since the analysis of the uploaded test details can take a few seconds (depending on the current server load) and 
Teamscale needs this data to be available in order to perform the Test Impact Analysis this service may return `null` 
to indicate, that the test details are not yet available. The client should periodically retry this call until it 
succeeds. If no tests are selected the result is an empty list instead.

### 3. Upload TESTWISE_COVERAGE report

During the test execution the coverage produced by each test case has to be saved separately.
After the execution the collected testwise coverage needs to be converted to the following format.
If possible the covered lines can already be compressed to method level, but this step is optional and only needed in 
order to reduce the file size of the final report.

```XML
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<report>
    <test uniformPath="[engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[method:systemTest()]">
        <path name="com/example/project">
            <file name="Calculator.java">
                <lines nr="13-15,20,24-30"/>
                ...
            </file>
            ...
        </path>
        ...
    </test>
    <test uniformPath="[engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[method:testAdd()]">
        ...
    </test>
    ...
</report>
```

The report may then be uploaded to Teamscale via the `Report Upload` REST-API by specifying `TESTWISE_COVERAGE` as 
report format.

## Rest API

To authorize the request Teamscale requires you to use Basic Authentication.
This means that the request should contain an `Authentication` header with the content `Basic ` followed by a Base64 
encoded version of `userName:userAccessToken`.

___
### Test Impact

  Returns the tests, which should be executed for the given set of changes. The set of changes is described by two 
  commits: The baseline commit and the end commit. All code changes after the baseline (exclusive) and before 
  end (inclusive) are considered. Also tests which have been changed in between are considered for execution.

  The `Accept` and `Content-Type` headers should be set to `application/json` for this request.

* **URL**

  p/{projectName}/test-impact

* **Method:**

  `GET`

*  **URL Parameters**

   `projectName` Project identifier within Teamscale

*  **Query Parameters**

   `baseline=[timestamp]` *(optional)* The baseline timestamp. A long value of the timestamp in milliseconds. 
       Only changes to the code after the baseline will be considered for execution. If not specified all not yet covered 
       changes will be considered.

   `end=[branch]:[timestamp]` Identifies the commit that has been used to build the system that we are about to test.

   `partitions=[string]` The partition to get impacted tests for. Multiple partitions could be specified as a comma 
        separated list (Not recommended).

* **Success Response:**

  * **Code:** 200 <br />
    **Content:**
    Returns a list of external IDs.
    ```json
    [
      "[engine:junit-jupiter]/[class:com.example.JUnit5Test]/[method:myFirstTest(org.junit.jupiter.api.TestInfo)]",
      "[engine:junit-vintage]/[runner:com.example.JUnit4Test]/[test:testAdd(com.example.project.JUnit4Test)]"
    ]
    ```

    Since the analysis of the uploaded test details can take a few seconds (depending on the current server load) and 
    Teamscale needs this data to be available in order to perform the test impact analysis this service may return `null` 
    to indicate, that the test details are not yet available. The client should periodically retry this call until it 
    succeeds. 


* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `failure`

___

### Report Upload

  Allows to upload external reports to Teamscale.

* **URL**

  /p/{projectName}/external-report/

* **Method:**

  `POST`

*  **URL Params**

   `projectName` Project identifier within Teamscale

*  **Query Params**

   `format=[TESTWISE_COVERAGE, TEST_LIST, JUNIT, JACOCO, GCOV, ...]` The format of the uploaded report

   `t=[branch]:[timestamp]` Identifies the commit which is associated with the upload (e.g. the commit for which coverage has been collected)

   `adjusttimestamp=[true or false]` *(optional)* Increases the timestamp in case a commit at the same time already exists.
   
   `movetolastcommit=[true or false]` *(optional)* Moves the timestamp right after the last commit.

   `partition=[string]` The partition to upload the report to. This typically identifies a set of tests e.g. "Unit Tests".

   `message=[string]` The commit message to show for this artificial report commit.

* **Data Params** (Multipart form data)

  `report` The UTF-8 encoded content of the report file.

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `success`

* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `failure`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `failure`

___

