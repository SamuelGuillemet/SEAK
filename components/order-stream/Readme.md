# Order Stream

The Order Stream is a component from the exchange system that is responsible for receiving orders from the client and verifying their integrity. It utilizes Apache Kafka for communication and integrates with Micronaut for Kafka-related annotations and dependencies. This README provides detailed documentation for understanding and using the Order Stream.

## Table of Contents

- [Overview](#overview)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Metrics](#metrics)
- [Running the Order Stream](#running-the-order-stream)
- [Testing the Order Stream](#testing-the-order-stream)

## Overview

The Order Stream is responsible for receiving orders from the client and verifying their integrity. It uses Kafka Streams to subscribe to a Kafka topic for incoming orders, processes them, and sends the resulting orders to another Kafka topic. The component verify the integrity using a Redis database to check user and stock balances.

## Dependencies

The Order Stream relies on the following dependencies:

- **Micronaut:** Micronaut is used for Kafka-related annotations and dependency injection.
- **Apache Kafka:** Kafka is used for messaging and communication between components.
- **SLF4J:** The Simple Logging Facade for Java is used for logging within the application.
- **Micrometer:** Micrometer is used for collecting and exposing metrics related to the Order Stream's performance.
- **Redis:** Redis is used for verifying the integrity of an order.

## Configuration

The Order Stream is configured using Micronaut configuration properties.

It depends on the following configuration propertie files:
- [`application.yml`](src/main/resources/application.yml): The main configuration file for the Order Stream.
- [`kafka.yml`](/config/common/kafka.yml): The configuration file for Kafka-related properties.
- [`redis.yml`](/config/common/redis.yml): The configuration file for Redis-related properties.
- [`monitoring.yml`](/config/common/monitoring.yml): The configuration file for Micrometer-related properties.

## Metrics

Micrometer is employed for collecting and exposing metrics related to the Order Stream's performance. The following metrics are captured:

- **Order Stream Check Integrity Metric:**
  - Type: _Timer_
  - Metric Name: `order_stream_check_integrity`
  - Tags:
    - `symbol`: The symbol of the processed order.
    - `side`: The side (buy/sell) of the processed order.
    - `type`: The type (market/limit) of the processed order.
  - Description: This timer records the time taken to check the integrity of an order, providing insights into the performance of the integrity check.

- **Order Stream Rejected Orders Metric:**
  - Type: _Counter_
  - Metric Name: `order_stream_rejected_order`
  - Tags:
    - `symbol`: The symbol of the rejected order.
    - `side`: The side (buy/sell) of the rejected order.
    - `type`: The type (market/limit) of the rejected order.
    - `orderRejectReason`: The reason for rejecting the order.
  - Description: This counter increments each time an order is rejected due to insufficient funds or else. It helps monitor and analyze rejected order patterns.

- **Order Stream Accepted Orders Metric:**
  - Type: _Counter_
  - Metric Name: `order_stream_accepted_order`
  - Tags:
    - `symbol`: The symbol of the accepted order.
    - `side`: The side (buy/sell) of the accepted order.
    - `type`: The type (market/limit) of the accepted order.
  - Description: This counter increments each time an order is accepted. It helps monitor and analyze accepted order patterns.

The **Prometheus** endpoint is exposed at `/prometheus` can be used to view the metrics.

The exposed port for the application is `10003`.

## Running the Order Stream

The Order Stream can be run using the following command:

```bash
$> ./gradlew components:order-stream:run
```

## Testing the Order Stream

The Order Stream can be tested using the following command:

```bash
$> ./gradlew components:order-stream:test
```

You can also generate a code coverage report using the following command:

```bash
$> ./gradlew components:order-stream:jacocoTestReport
```

This will generate a code coverage report at [`components/order-stream/build/reports/jacoco/test/html/index.html`](/components/order-stream/build/reports/jacoco/test/html/index.html).
