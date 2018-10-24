
### Gradle

Most of the work can be automated by using the `teamscale` Gradle plugin.

**Requirements:**
 - Gradle 4.6
 - Tests are executed via JUnit Platform

Add this to the top of your root `build.gradle` file.
```groovy
buildscript {
	repositories {
    mavenCentral()
		maven { url 'https://share.cqse.eu/public/maven/' }
	}
	dependencies {
		classpath 'eu.cqse:teamscale-gradle-plugin:0.2.0'
	}
}

apply plugin: 'teamscale'

teamscale {
    server {
        url = 'https://mycompany.com/teamsale'
        userName = 'build'
        userAccessToken = '7fa5.....'
        project = 'example-project-id'
    }
    testImpactMode = true

    // The following is optional. By default the plugin looks for a git
    // repository in the project's root directory and takes the branch and
    // timestamp of the currently checked out commit.
    commit {
        branchName = 'master'
        timestamp = 1521800427000L // Timestamp in milliseconds
    }
    

    // The commit message to show for the uploaded reports (optional, Default: 'Gradle Upload')
    message = 'Gradle Upload'

    // The reports that should be uploaded to Teamscale
    report {
        // The partition in Teamscale
        partition = 'Unit Tests'
        
        // Uploads testwise coverage
        testwiseCoverage()
        
        // Uploads jUnit reports
        jUnit()
        
        // Uploads google closure coverage (together with the java coverage) as testwise coverage
        googleClosureCoverage { js ->
            // The directory in which the google closure coverage files reside after the test
            js.destination = files("$rootDir/engine/com.teamscale.test/ui-test-coverage")
            // Ant patterns for which files to include/exclude in the final report
            js.excludes = ["**/google-closure-library/**", "**.soy.generated.js", "soyutils_usegoog.js"]
        }
    }
    
    // For performance reasons the classes to be instrumented and analyzed can be filtered to only include the relevant 
    // ones. Includes and excludes have to be specified as wildcard pattern with ? and * as placeholders. (Optional)
    agent {
        excludes = [
            '*.generated.*'
        ]
        includes = [
            'com.package.my.*',
            'org.mine.MyClass'
        ]
        // Where to store the JaCoCo exec file (Optional)
        executionData = file("...")
        // Allows to dump all loaded classes to a directory and use them to generate the report
        // Might be needed when doing additional class transformations e.g. by another profiler
        // (Optional)
        dumpClasses = true
        dumpDirectory = file("...")
    }
}
```

Any of those settings can be overridden in the test task's closure. This comes in handy if you have multiple test tasks.

```groovy
task unitTest(type: Test) {
    useJUnitPlatform {
        excludeTags 'integration'
    }
    teamscale.report.partition = 'Unit Tests'
}

task integrationTest(type: Test) {
    useJUnitPlatform {
        includeTags 'integration'
    }
    teamscale.report.partition = 'Integration Tests'
}
```
The Teamscale plugin generates a special task for every test task you define suffixed with `Impacted` e.g. `unitTestImpacted`.
This task automatically uploads the available tests to Teamscale and runs only the impacted tests for the last commit.
Afterwards `TESTWISE_COVERAGE` and `JUNIT` reports are uploaded to Teamscale. Setting the `--run-all-tests` allows to run all tests and still generate a `TESTWISE_COVERAGE` report for all tests.

Uploading reports can also be triggered independently of the `Impacted` task with the Gradle task `unitTestReportUpload`. By starting Gradle with `-x unitTestReportUpload` you can also disable the automatic upload.
