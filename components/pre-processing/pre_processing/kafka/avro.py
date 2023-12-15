import logging

from confluent_kafka.schema_registry import Schema, SchemaRegistryClient

from pre_processing.utils.loader import get_avro_schema, get_kafka_config

logger = logging.getLogger("pre_processing.kafka.avro")


class AvroService:
    SCHEMA_URL: str = get_kafka_config()["schema.registry.url"]
    SCHEMA_FILE = "market-data.avsc"

    @classmethod
    def register_schema(cls, schema_registry_url, schema_registry_subject, schema_str):
        sr = SchemaRegistryClient({"url": schema_registry_url})
        schema = Schema(schema_str, schema_type="AVRO")
        schema_id = sr.register_schema(
            subject_name=schema_registry_subject, schema=schema
        )
        logger.info(f"Schema {schema_registry_subject} registered")

        return schema_id

    @classmethod
    def update_schema(cls, schema_registry_url, schema_registry_subject, schema_str):
        sr = SchemaRegistryClient({"url": schema_registry_url})
        sr.delete_subject(schema_registry_subject)

        schema_id = cls.register_schema(
            schema_registry_url, schema_registry_subject, schema_str
        )
        return schema_id

    @classmethod
    def delete_schema(cls, schema_registry_url, schema_registry_subject):
        sr = SchemaRegistryClient({"url": schema_registry_url})
        sr.delete_subject(schema_registry_subject)

    @classmethod
    def get_schema_from_schema_registry(
        cls, schema_registry_url: str, schema_registry_subject: str
    ):
        sr = SchemaRegistryClient({"url": schema_registry_url})
        latest_version = sr.get_latest_version(schema_registry_subject)

        return sr, latest_version

    @classmethod
    def get_schema_from_file(
        cls,
        schema_registry_subject: str,
        schema_file_path: str,
    ):
        sr = SchemaRegistryClient({"url": schema_registry_subject})
        schema_str = get_avro_schema(schema_file_path)
        return sr, schema_str
