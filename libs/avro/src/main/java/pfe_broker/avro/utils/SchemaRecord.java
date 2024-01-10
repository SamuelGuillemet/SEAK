package pfe_broker.avro.utils;

import org.apache.avro.Schema;

public record SchemaRecord(Schema schema, String topicName) {

}
