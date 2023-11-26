# Logger library

The config for this log4j2 logger is at [log42j.yml](../../config/shared/log4j2.yml).

The defined logger is called `pfe_broker`.

## Use

You can import the logger with the following:

```java
import static pfe_broker.log.Log.LOG;
```

Be sure to add the log4j2 and lib dependencies inside the `build.gradle` file as following:

```groovy
dependencies {
    implementation project(":libs:log")
    // Log4J
    implementation group: 'org.apache.logging.log4j', name: 'log4j-core', version: '2.21.1'
    implementation group: 'org.apache.logging.log4j', name: 'log4j-api', version: '2.21.1'

    // Rest of dependencies
}
```
