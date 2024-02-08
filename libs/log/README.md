# Logger library

The config for this log4j2 logger is at [log42j.yml](../../config/shared/log4j2.yml).

The defined logger is called `seak`.

## Use

You can import the logger with the following:

```java
import static io.seak.log.Log.LOG;
```

Be sure to add the log4j2 and lib dependencies inside the `build.gradle` file as following:

```groovy
dependencies {
    implementation project(":libs:log")
    // Log4J
    implementation group: 'org.apache.logging.log4j', name: 'log4j-core', version: '2.22.0'
    runtimeOnly group: 'org.apache.logging.log4j', name: 'log4j-api', version: '2.22.0'
    runtimeOnly group: 'org.apache.logging.log4j', name: 'log4j-slf4j2-impl', version: '2.22.0'

    // Rest of dependencies
}
```
