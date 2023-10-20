import logging
from typing import Any

from confluent_kafka import Producer as ConfluentProducer

logger = logging.getLogger("pfe_preprocessing.kafka.producer")


class AIOProducer:
    def __init__(self, configs: dict[str, str]):
        self._producer = ConfluentProducer(configs)

    def on_delivery(self, err, msg):
        if err:
            logger.error(f"Message delivery failed: {err}")
        else:
            pass

    def produce(self, topic: str, value: Any, key: Any = None):
        """
        An awaitable produce method.
        """
        self._producer.produce(
            topic, key=key, value=value, on_delivery=self.on_delivery
        )

    def flush(self, timeout: float = 5.0) -> None:
        """
        Wait until all messages have been delivered
        """
        return self._producer.flush(timeout)
