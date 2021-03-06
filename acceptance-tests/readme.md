# Acceptance Tests

This module contains the acceptance tests for the Bitbucket Server integration plugin, including the UI tests.

## Running the tests

The Jenkins [Acceptance Test Harness framework](https://github.com/jenkinsci/acceptance-test-harness) runs an instance 
of Jenkins per test and installs the plugin under test using the `.hpi` file in the `../target` folder, defined using 
the `LOCAL_JARS` environment variable. For example, using `maven-surefire-plugin`:

```
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.0.0-M4</version>
    <configuration>
        <reuseForks>false</reuseForks>
        <environmentVariables>
            ...
            <LOCAL_JARS>../target/atlassian-bitbucket-server-integration.hpi</LOCAL_JARS>
            ...
        </environmentVariables>
    </configuration>
</plugin>
```

To build the `.hpi` file, build the plugin from the project root folder:

```
mvn clean package -DskipTests
```

---
If you see the following error when running the tests, it means you haven't built the plugin:

```
java.lang.IllegalArgumentException: Unable to honor LOCAL_JARS environment variable

	at org.jenkinsci.test.acceptance.update_center.LocalOverrideUpdateCenterMetadataDecoratorImpl.decorate(LocalOverrideUpdateCenterMetadataDecoratorImpl.java:83)
	at org.jenkinsci.test.acceptance.update_center.CachedUpdateCenterMetadataLoader.get(CachedUpdateCenterMetadataLoader.java:48)
	at org.jenkinsci.test.acceptance.update_center.MockUpdateCenter.ensureRunning(MockUpdateCenter.java:100)
	at org.jenkinsci.test.acceptance.po.PluginManager.checkForUpdates(PluginManager.java:87)
	at org.jenkinsci.test.acceptance.po.PluginManager.installPlugins(PluginManager.java:182)
...
Caused by: java.lang.IllegalArgumentException: Plugin file does not exist: <path-to-project-root->/acceptance-tests/../target/atlassian-bitbucket-server-integration.hpi
	at org.jenkinsci.test.acceptance.update_center.LocalOverrideUpdateCenterMetadataDecoratorImpl.override(LocalOverrideUpdateCenterMetadataDecoratorImpl.java:91)
	at org.jenkinsci.test.acceptance.update_center.LocalOverrideUpdateCenterMetadataDecoratorImpl.decorate(LocalOverrideUpdateCenterMetadataDecoratorImpl.java:81)
	... 27 more
```
---

Then run the tests from inside the `acceptance-tests` folder:

```
cd acceptance-tests
mvn verify
```

This will run all the tests.

You can also run individual tests inside your IDE of choice (e.g. Intellij IDEA).

## Changing the browser and Jenkins version

To override the browser for WebDriver tests (currently set to `firefox-container`, meaning the tests will run in Firefox 
inside a Selenium-provided container), pass in the `BROWSER` environment variable when running the tests.

```
BROWSER=safari mvn verify
```
For more details and the full list of supported browsers, see [the docs](https://github.com/jenkinsci/acceptance-test-harness/blob/master/docs/BROWSER.md).

To override the Jenkins version used for the tests, pass in the `JENKINS_VERSION` environment variable:

```
JENKINS_VERSION=2.176.4 mvn verify
```

For more on managing the versions of Jenkins and plugins under test, see [this doc](https://github.com/jenkinsci/acceptance-test-harness/blob/master/docs/SUT-VERSIONS.md).

## Running against an existing Jenkins instance

Having the test framework start up and set up a Jenkins instance per test is very slow, so sometimes we might want to 
run the tests against an external running instance of Jenkins. To do that, use the following environment variables:

```
TYPE=existing
JENKINS_URL=<jenkins-instance-url> (e.g. http://localhost:8080/)
```

(e.g. if running the tests from inside Intellij, pass the environment variables in the run configuration)
This is, for example, handy when developing new tests and having to run the test over and over again, so that you don't 
have to wait for Jenkins to startup every single time.

### Running an external Jenkins instance for testing

Unfortunately, running a vanilla Jenkins instance (e.g. using a downloaded `war` or the maven `hpi` plugin) won't work 
because of some missing plugins that the Jenkins acceptance test framework seems to rely on, e.g.:

```
java.lang.RuntimeException: Test suite requires in pre-installed Jenkins plugin https://wiki.jenkins-ci.org/display/JENKINS/Form+Element+Path+Plugin
```

The surest way to run a Jenkins instance for these tests is to clone the Jenkins 
[Acceptance Test Harness framework](https://github.com/jenkinsci/acceptance-test-harness) and run the `jut-server.sh` script:

```
git clone git@github.com:jenkinsci/acceptance-test-harness.git
cd acceptance-test-harness
./jut-server.sh
```

For more details see: [prelaunching Jenkins under test](https://github.com/jenkinsci/acceptance-test-harness/blob/master/docs/PRELAUNCH.md)

## Developing new tests

---
Note on developing in Intellij (similar steps may be required with other IDEs too):  
The `acceptance-tests` Maven module may not be automatically picked up and imported by Intellij when you import the 
project. To import the module, right click on the `pom.xml` in the `acceptance-tests` module and click `Add as Maven Project`.
---

### Writing a new JUnit test

The easiest way to develop a new test is to extend [AbstractJUnitTest](https://github.com/jenkinsci/acceptance-test-harness/blob/master/src/main/java/org/jenkinsci/test/acceptance/junit/AbstractJUnitTest.java).
That way, you'll get a bunch of things already injected into your test, like the `JenkinsAcceptanceTestRule` which is 
responsible for starting up a Jenkins instance for testing, among other things.  
For more details, see [this doc](https://github.com/jenkinsci/acceptance-test-harness/blob/master/docs/JUNIT.md).
