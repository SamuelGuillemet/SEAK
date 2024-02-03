# Order Book

The Order Book is a component of the exchange system that maintains the list of orders for each symbol. It is responsible for adding new orders to the order book, matching orders, and removing orders from the order book. It utilizes Apache Kafka for communication and integrates with Micronaut for Kafka-related annotations and dependencies. This README provides detailed documentation for understanding and using the Order Book.

## Table of Contents

- [Overview](#overview)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Metrics](#metrics)
- [Running the Order Book](#running-the-order-book)
- [Testing the Order Book](#testing-the-order-book)

## Overview

The Order Book is responsible for maintaining the list of orders for each symbol. It subscribes to Kafka topics for order book requests, processes them, and sends the resulting order book updates to another Kafka topic. The component also handles the matching of orders and the removal of orders from the order book when they are filled.

The other role of this component is to allow users to subscribe to market data updates for a particular symbol. It subscribes to a Kafka topic for market data requests, processes them, and sends the resulting market data responses to another Kafka topic. The component also handles the unsubscription of users from market data updates.

## Dependencies

The Order Book relies on the following dependencies:

- **Micronaut:** Micronaut is used for Kafka-related annotations and dependency injection.
- **Apache Kafka:** Kafka is used for messaging and communication between components.
- **SLF4J:** The Simple Logging Facade for Java is used for logging within the application.
- **Micrometer:** Micrometer is used for collecting and exposing metrics related to the Order Book's performance.
- **Redis:** Redis is used for verifying the integrity of an order book modification request.

## Configuration

The Order Book is configured using Micronaut configuration properties.

It depends on the following configuration propertie files:
- [`application.yml`](src/main/resources/application.yml): The main configuration file for the Order Book.
- [`kafka.yml`](/config/common/kafka.yml): The configuration file for Kafka-related properties.
- [`redis.yml`](/config/common/redis.yml): The configuration file for Redis-related properties.
- [`monitoring.yml`](/config/common/monitoring.yml): The configuration file for Micrometer-related properties.

## Metrics

Micrometer is employed for collecting and exposing metrics related to the Order Book's performance. The following metrics are captured:

- **Order Book Handle Request Metric:**
  - Type: _Timer_
  - Metric Name: `order_book_handle_order`
  - Tags:
    - `symbol`: The symbol of the order book request.
    - `requestType`: The type of the order book request (NEW, MODIFY, CANCEL).
  - Description: This timer records the time taken to handle an order book request, providing insights into the performance of the order book.

- **Order Book Match Orders Metric:**
  - Type: _Timer_
  - Metric Name: `order_book_match_orders`
  - Tags:
    - `symbol`: The symbol of the order book request.
  - Description: This timer records the time taken to match orders, providing insights into the performance of the matching algorithm.

- **Order Book Market Data Request Metric:**
  - Type: _Timer_
  - Metric Name: `order_book_market_data_request`
  - Tags:
    - `subscriptionRequest`: The type of the market data request (SNAPSHOT, SUBSCRIBE, UNSUBSCRIBE).
  - Description: This timer records the time taken to handle a market data request, providing insights into the performance of the market data request.

- **Order Book Volume Metric:**
  - Type: _Gauge_
  - Metric Name: `order_book_volume_order_book`
  - Tags:
    - `symbol`: The symbol of the order book request.
    - `side`: The side (BUY/SELL) of the order book request.
  - Description: This gauge records the total volume of the order book, providing insights into the liquidity of the market.

- **Order Book Market Data Subscriptions Metric:**
  - Type: _Gauge_
  - Metric Name: `order_book_market_data_subscriptions`
  - Description: This gauge records the number of market data subscriptions for all symbols, providing insights into the number of users subscribed to market data updates.

- **Order Book Trades Metric:**
  - Type: _Counter_
  - Metric Name: `order_book_trades`
  - Tags:
    - `symbol`: The symbol of the order book request.
  - Description: This counter increments each time a trade is executed, providing insights into the liquidity of the market.

- **Order Book Rejected Orders Metric:**
  - Type: _Counter_
  - Metric Name: `order_book_rejected`
  - Tags:
    - `symbol`: The symbol of the order book request.
    - `requestType`: The type of the order book request (NEW, MODIFY, CANCEL).
  - Description: This counter increments each time an order is rejected due to any issue. It helps monitor and analyze rejected order patterns.

- **Order Book Response Metric:**
  - Type: _Counter_
  - Metric Name: `order_book_responses`
  - Tags:
    - `symbol`: The symbol of the order book request.
    - `requestType`: The type of the order book request (NEW, MODIFY, CANCEL).
  - Description: This counter increments each time a response is sent to the client, providing insights into the performance of the order book.

- **Order Book Market Data Response Metric:**
  - Type: _Counter_
  - Metric Name: `order_book_market_data_responses`
  - Tags:
    - `symbol`: The symbol of the order book request.
  - Description: This counter increments each time a market data response is sent to the client, providing insights into the performance of the market data update.

- **Order Book Market Data Rejected Metric:**
  - Type: _Counter_
  - Metric Name: `order_book_market_data_rejected`
  - Tags:
    - `symbol`: The symbol of the order book request.
    - `reason`: The reason for the rejection of the market data request.
  - Description: This counter increments each time a market data request is rejected due to any issue. It helps monitor and analyze rejected market data request patterns.


The **Prometheus** endpoint is exposed at `/prometheus` can be used to view the metrics.

The exposed port for the application is `10002`.

## Running the Order Book

The Order Book can be run using the following command:

```bash
$> ./gradlew components:order-book:run
```

## Testing the Order Book

The Order Book can be tested using the following command:

```bash
$> ./gradlew components:order-book:test
```

You can also generate a code coverage report using the following command:

```bash
$> ./gradlew components:order-book:jacocoTestReport
```

This will generate a code coverage report at [`components/order-book/build/reports/jacoco/test/html/index.html`](/components/order-book/build/reports/jacoco/test/html/index.html).
