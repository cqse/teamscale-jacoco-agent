
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
		classpath 'com.teamscale:teamscale-gradle-plugin:0.3.0'
	}
}

apply plugin: 'teamscale'

teamscale {
    server {
        url = 'https://mycompany.com/teamsale/'
        userName = 'build'
        userAccessToken = '7fa5.....'
        project = 'example-project-id'
    }

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
        
        // Uploads google closure coverage (together with the java coverage) as testwise coverage
        googleClosureCoverage { js ->
            // The directory in which the google closure coverage files reside after the test
            js.destination = files("$rootDir/engine/com.teamscale.test/ui-test-coverage")
            // Ant patterns for which files to include/exclude in the final report
            js.excludes = ["**/google-closure-library/**", "**.soy.generated.js", "soyutils_usegoog.js"]
        }
    }
    
    //
    agent {
        // Where to store the JaCoCo exec file and other test artifacts (Optional)
        destination = file("...")
        
        // Configures the test runner to additionally notify a remote agent
        useRemoteAgent()
    }
}
```

Tests must be defined of type `TestImpacted`
Any of those settings can be overridden in the test task's closure. This comes in handy if you have multiple test tasks.

```groovy
tasks.register('unitTest', TestImpacted) {
    useJUnitPlatform {
        excludeTags 'integration'
    }
    teamscale.report.partition = 'Unit Tests'
}

tasks.register('integrationTest', TestImpacted) {
    useJUnitPlatform {
        includeTags 'integration'
    }
    teamscale.report.partition = 'Integration Tests'
}
```

When executing the tests you can add the `--impacted` flag to only execute impacted tests.
This task automatically uploads the available tests to Teamscale and runs only the impacted tests for the last commit.
Afterwards a `TESTWISE_COVERAGE` report is generated. Setting the `--run-all-tests` allows to run all tests and still generate a `TESTWISE_COVERAGE` report for all tests.

Uploading reports can be triggered with the Gradle task `teamscaleReportUpload`.
