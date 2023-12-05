# Avro library

The avro schemas are defined inside the avro folder inside config.

We have definied the following schemas:

- [MarketData](../../config/avro/market-data.avsc)
- [OrderRejectReason](../../config/avro/order-rejected-reason.avsc)
- [Order](../../config/avro/order.avsc)
- [RejectedOrder](../../config/avro/rejected-order.avsc)
- [Side](../../config/avro/side.avsc)
- [Trade](../../config/avro/trade.avsc)


## Use

You can import each entity with this kind of import:

```java
import pfe_broker.avro.Order;
```

Bes sure to add avro and lib dependencies inside the `build.gradle` file as following:

```groovy
repositories {
    // Rest of repositories
    maven { url confluentUrl }
}
dependencies {
    implementation project(":libs:avro")
    // Avro
    implementation group: 'org.apache.avro', name: 'avro', version: '1.11.1'

    // Rest of dependencies
}
```

If you want to use avro inside kafka stream you will need the following:

```groovy
dependencies {
    // Avro serde
    implementation group: 'io.confluent', name: 'kafka-streams-avro-serde', version: '7.5.1'

    // Rest of dependencies
}
```

You can then define the following:

```java
import pfe_broker.avro.Order;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
```

```java
protected SpecificAvroSerde<Order> orderAvroSerde() {
    SpecificAvroSerde<Order> orderAvroSerde = new SpecificAvroSerde<>();

    Map<String, String> serdeConfig = new HashMap<>();
    serdeConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, BasicConfig.SCHEMA_REGISTRY_URL);
    orderAvroSerde.configure(serdeConfig, false); // False when used as a value, true otherwise
    return orderAvroSerde;
}
```

This can be used later to build a KStream:

```java
KStream<Integer, Order> orderStream = builder.stream("example-stream",
        Consumed.with(this.keySerde, this.orderAvroSerde()));
```
