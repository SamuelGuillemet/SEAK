import logging

from confluent_kafka.admin import AdminClient as _AdminClient
from confluent_kafka.admin import NewTopic

from pre_processing.constant import MARKET_DATA_PARTIONS

logger = logging.getLogger("pre_processing.kafka.admin")


class AdminClient:
    def __init__(self, configs: dict[str, str]):
        self.admin_client = _AdminClient(configs)

    def create_topics(self, topics: list[NewTopic]):
        """Create topics"""

        new_topics = [
            NewTopic(topic, num_partitions=MARKET_DATA_PARTIONS, replication_factor=1)
            for topic in topics
        ]
        # Call create_topics to asynchronously create topics, a dict
        # of <topic,future> is returned.
        fs = self.admin_client.create_topics(new_topics)

        # Wait for operation to finish.
        # Timeouts are preferably controlled by passing request_timeout=15.0
        # to the create_topics() call.
        # All futures will finish at the same time.
        for topic, f in fs.items():
            try:
                f.result()  # The result itself is None
                logger.debug(f"Topic {topic} created")
            except Exception as e:
                logger.error(f"Failed to create topic {topic}: {e}")

    def delete_topics(self, topics: list[str], operation_timeout: float = 30.0):
        """delete topics"""

        # Call delete_topics to asynchronously delete topics, a future is returned.
        # By default this operation on the broker returns immediately while
        # topics are deleted in the background. But here we give it some time (30s)
        # to propagate in the cluster before returning.
        #
        # Returns a dict of <topic,future>.
        fs = self.admin_client.delete_topics(
            topics, operation_timeout=operation_timeout
        )

        # Wait for operation to finish.
        for topic, f in fs.items():
            try:
                f.result()  # The result itself is None
                logger.debug(f"Topic {topic} deleted")
            except Exception as e:
                logger.info(f"Failed to delete topic {topic}: {e}")
