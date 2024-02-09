import logging
from typing import Any

from confluent_kafka import SerializingProducer
from confluent_kafka.schema_registry.avro import AvroSerializer

from pre_processing.kafka.avro import AvroService

logger = logging.getLogger("pre_processing.kafka.producer")


class AIOProducer:
    def __init__(self, configs: dict[str, str]):
        sr, schema_str = AvroService.get_schema_from_file(
            AvroService.SCHEMA_URL, AvroService.SCHEMA_FILE
        )
        value_avro_serializer = AvroSerializer(
            schema_registry_client=sr,
            schema_str=schema_str,
            conf={"auto.register.schemas": False},
        )
        self._producer = SerializingProducer(
            {**configs, "value.serializer": value_avro_serializer}
        )

    def on_delivery(self, err, msg):
        if err:
            logger.error(f"Message delivery failed: {err}")

    def produce(
        self, topic: str, value: Any, key: Any = None, partition: int = -1
    ) -> None:
        """
        An awaitable produce method.
        """
        self._producer.produce(
            topic,
            key=key,
            value=value,
            on_delivery=self.on_delivery,
            partition=partition,
        )

    def flush(self, timeout: float = 5.0) -> None:
        """
        Wait until all messages have been delivered
        """
        return self._producer.flush(timeout)
