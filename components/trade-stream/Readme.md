# Trade Stream

The Trade Stream is a component of the exchange system that is responsible for receiving trades from either the Market Matcher or the Order Book and verifying their integrity. It utilizes Apache Kafka for communication and integrates with Micronaut for Kafka-related annotations and dependencies. This README provides detailed documentation for understanding and using the Trade Stream.

## Table of Contents

- [Overview](#overview)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Metrics](#metrics)
- [Running the Trade Stream](#running-the-trade-stream)
- [Testing the Trade Stream](#testing-the-trade-stream)

## Overview

The Trade Stream is responsible for receiving trades from either the Market Matcher or the Order Book and verifying their integrity. It uses Kafka Streams to subscribe to a Kafka topic for incoming trades, processes them, and sends the resulting trades to another Kafka topic. The component verify the integrity using a Redis database to check user and stock balances. If a trade is rejected, the corresponding order is sent to the rejected order topic.

## Workflow

![alt text](/docs/imgs/trade-stream.png)

The Trade Integrity Check Service Flow Diagram outlines the process of ensuring the correctness and validity of trades in a trading system.

Depending on the type of trade—whether market or limit—the system executes specific integrity checks.
- For market trades, the system verifies the user's fund availability, increments the user's stock quantity and decrements the user's fund quantity accordingly for buy orders. For sell orders, it increments the stock quantity.
- For limit trades, it increments user stock quantity for buy orders. For sell orders, it increments the user fund's.

If a trade is rejected, the system logs the reason for rejection and send the corresponding orders to the rejected order topic; otherwise, it records the acceptance of the trade.

## Dependencies

The Trade Stream relies on the following dependencies:

- **Micronaut:** Micronaut is used for Kafka-related annotations and dependency injection.
- **Apache Kafka:** Kafka is used for messaging and communication between components.
- **SLF4J:** The Simple Logging Facade for Java is used for logging within the application.
- **Micrometer:** Micrometer is used for collecting and exposing metrics related to the Trade Stream's performance.
- **Redis:** Redis is used for verifying the integrity of a trade.

## Configuration

The Trade Stream is configured using Micronaut configuration properties.

It depends on the following configuration propertie files:

- [`application.yml`](src/main/resources/application.yml): The main configuration file for the Trade Stream.
- [`kafka.yml`](/config/common/kafka.yml): The configuration file for Kafka-related properties.
- [`redis.yml`](/config/common/redis.yml): The configuration file for Redis-related properties.
- [`monitoring.yml`](/config/common/monitoring.yml): The configuration file for Micrometer-related properties.

## Metrics

Micrometer is employed for collecting and exposing metrics related to the Trade Stream's performance. The following metrics are captured:

- **Trade Stream Check Integrity Metric:**
  - Type: _Timer_
  - Metric Name: `trade_stream_check_integrity`
  - Tags:
    - `symbol`: The symbol of the processed trade.
    - `side`: The side (buy/sell) of the processed trade.
    - `type`: The type (market/limit) of the processed trade.
  - Description: This timer records the time taken to check the integrity of a trade, providing insights into the performance of the integrity check.

- **Trade Stream Rejected Trades Metric:**
  - Type: _Counter_
  - Metric Name: `trade_stream_rejected_trade`
  - Tags:
    - `symbol`: The symbol of the rejected trade.
    - `side`: The side (buy/sell) of the rejected trade.
    - `type`: The type (market/limit) of the rejected trade.
    - `tradeRejectReason`: The reason for rejecting the trade.
  - Description: This counter increments each time a trade is rejected due to insufficient funds or else. It helps monitor and analyze rejected trade patterns.

- **Trade Stream Trades Accepted Metric:**
  - Type: _Counter_
  - Metric Name: `trade_stream_accepted_trade`
  - Tags:
    - `symbol`: The symbol of the accepted trade.
    - `side`: The side (buy/sell) of the accepted trade.
    - `type`: The type (market/limit) of the accepted trade.
  - Description: This counter increments each time a trade is accepted. It helps monitor and analyze accepted trade patterns.

The **Prometheus** endpoint is exposed at `/prometheus` can be used to view the metrics.

The exposed port for the application is `10005`.

## Running the Trade Stream

The Trade Stream can be run using the following command:

```bash
$> ./gradlew components:trade-stream:run
```

## Testing the Trade Stream

The Trade Stream can be tested using the following command:

```bash
$> ./gradlew components:trade-stream:test
```

You can also generate a code coverage report using the following command:

```bash
$> ./gradlew components:trade-stream:jacocoTestReport
```

This will generate a code coverage report at [`components/trade-stream/build/reports/jacoco/test/html/index.html`](/components/trade-stream/build/reports/jacoco/test/html/index.html).
