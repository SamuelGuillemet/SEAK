# Market Matcher

The Market Matcher is a component of the PFE Broker system designed to match incoming orders with market data and generate trade executions. It utilizes Apache Kafka for communication and integrates with Micronaut for Kafka-related annotations and dependencies. This README provides detailed documentation for understanding and using the Market Matcher.

## Table of Contents

- [Overview](#overview)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Metrics](#metrics)
- [Running the Market Matcher](#running-the-market-matcher)
- [Testing the Market Matcher](#testing-the-market-matcher)

## Overview

The Market Matcher is responsible for matching incoming orders with market data. It subscribes to Kafka topics for accepted orders, processes them, and sends the resulting trades to another Kafka topic. The component also handles the rejection of orders with unknown symbols.

## Dependencies

The Market Matcher relies on the following dependencies:

- **Micronaut:** Micronaut is used for Kafka-related annotations and dependency injection.
- **Apache Kafka:** Kafka is used for messaging and communication between components.
- **SLF4J:** The Simple Logging Facade for Java is used for logging within the application.
- **Micrometer:** Micrometer is used for collecting and exposing metrics related to the Market Matcher's performance.

## Configuration

The Market Matcher is configured using Micronaut configuration properties. 

It depends on the following configuration propertie files:
- [`application.yml`](src/main/resources/application.yml): The main configuration file for the Market Matcher.
- [`kafka.yml`](/config/common/kafka.yml): The configuration file for Kafka-related properties.
- [`monitoring.yml`](/config/common/monitoring.yml): The configuration file for Micrometer-related properties.

## Metrics

Micrometer is employed for collecting and exposing metrics related to the Market Matcher's performance. The following metrics are captured:

- **Processing Time Metric:**
  - Type: _Timer_
  - Metric Name: `market_matcher_process_order`
  - Tags:
    - `symbol`: The symbol of the processed order.
    - `side`: The side (buy/sell) of the processed order.
  - Description: This timer records the time taken to process an order, providing insights into the performance of the matching algorithm.

- **Rejected Orders Metric:**
  - Type: _Counter_
  - Metric Name: `market_matcher_rejected_order`
  - Tags:
    - `symbol`: The symbol of the rejected order.
    - `side`: The side (buy/sell) of the rejected order.
  - Description: This counter increments each time an order is rejected due to an unknown symbol. It helps monitor and analyze rejected order patterns.

The **Prometheus** endpoint is exposed at `/prometheus` can be used to view the metrics.

The exposed port for the application is `10001`.

## Running the Market Matcher

The Market Matcher can be run using the following command:

```bash
$> ./gradlew components:market-matcher:run
```

## Testing the Market Matcher

The Market Matcher can be tested using the following command:

```bash
$> ./gradlew components:market-matcher:test
```

You can also generate a code coverage report using the following command:

```bash
$> ./gradlew components:market-matcher:jacocoTestReport
```

This will generate a code coverage report at [`components/market-matcher/build/reports/jacoco/test/html/index.html`](/components/market-matcher/build/reports/jacoco/test/html/index.html).
