package io.seak.avro.utils;

import org.apache.avro.Schema;

public record SchemaRecord(Schema schema, String topicName) {}
