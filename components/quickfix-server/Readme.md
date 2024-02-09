# Quickfix Server

The Quickfix Server is a component of the exchange that is responsible for receiving orders from clients and sending responses back to them. It is implemented using the [Quickfix/J](https://www.quickfixj.org/) library. This README provides detailed documentation for understanding and using the Quickfix Server.

## Table of Contents

- [Overview](#overview)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Running the Quickfix Server](#running-the-quickfix-server)
- [Testing the Quickfix Server](#testing-the-quickfix-server)

## Overview

The Quickfix Server is responsible for receiving orders from clients and sending responses back to them. It is implemented using the [Quickfix/J](https://www.quickfixj.org/) library. The component is configured using a configuration file and a session settings file. It also verify if users are authorized to connect to the exchange. 

Moreover if the client disconnects from the exchange, the Quickfix Server will save the pending messages and resend them when the client reconnects.

## Dependencies

The Quickfix Server relies on the following dependencies:

- **Micronaut:** Micronaut is used for dependency injection.
- **Quickfix/J:** Quickfix/J is used for implementing the Quickfix Server.
- **Apache Kafka:** Kafka is used for messaging and communication between components.
- **SLF4J:** The Simple Logging Facade for Java is used for logging within the application.
- **Micrometer:** Micrometer is used for collecting and exposing metrics related to the Quickfix Server's performance.
- **PostgreSQL:** PostgreSQL is used for storing user credentials.

## Configuration

The Quickfix Server is configured using Micronaut configuration properties.

It depends on the following configuration propertie files:
- [`application.yml`](src/main/resources/application.yml): The main configuration file for the Quickfix Server.
- [`kafka.yml`](/config/common/kafka.yml): The configuration file for Kafka-related properties.
- [`monitoring.yml`](/config/common/monitoring.yml): The configuration file for Micrometer-related properties.
- [`data.yml`](/config/common/data.yml): The configuration file for PostgreSQL-related properties.
- [`quickfix.yml`](/config/common/quickfix.yml): The configuration file for Quickfix-related properties.

## Metrics

Micrometer is employed for collecting and exposing metrics related to the Quickfix Server's performance. The following metrics are captured:

- **Quickfix Server Users Connected Metric:**
  - Type: _Gauge_
  - Metric Name: `quickfix_server_users_connected`
  - Description: This gauge records the number of users connected to the Quickfix Server, providing insights into the number of users connected to the exchange.

- **Quickfix Server Messages Sent Metric:**
  - Type: _Counter_
  - Metric Name: `quickfix_server_messages_sent`
  - Tags:
    - `messageType`: The type of the message sent.
  - Description: This counter increments each time a message is sent by the Quickfix Server, providing insights into the number of messages sent.

- **Quickfix Server Messages Received Metric:**
  - Type: _Counter_
  - Metric Name: `quickfix_server_messages_received`
  - Tags:
    - `messageType`: The type of the message received.
  - Description: This counter increments each time a message is received by the Quickfix Server, providing insights into the number of messages received.

The **Prometheus** endpoint is exposed at `/prometheus` can be used to view the metrics.

The exposed port for the application is `10004`.

## Running the Quickfix Server

The Quickfix Server can be run using the following command:

```bash
$> ./gradlew components:quickfix-server:run
```

## Testing the Quickfix Server

The Quickfix Server can be tested using the following command:

```bash
$> ./gradlew components:quickfix-server:test
```

You can also generate a code coverage report using the following command:

```bash
$> ./gradlew components:quickfix-server:jacocoTestReport
```

This will generate a code coverage report at [`components/quickfix-server/build/reports/jacoco/test/html/index.html`](/components/quickfix-server/build/reports/jacoco/test/html/index.html).
