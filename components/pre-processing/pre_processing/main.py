"""
Main file to feed kafka with data from 2022-04-05
"""

import argparse
import logging
from typing import Dict, cast

import pandas as pd
from confluent_kafka.schema_registry.error import SchemaRegistryError

from pre_processing.constant import DATA_PATH, StockChartType
from pre_processing.data_agregation.helpers import save_date_data
from pre_processing.data_agregation.reader import gather_data
from pre_processing.data_completion.helpers import (
    bound_dataframe,
    preprocess_data,
    remove_ticker,
)
from pre_processing.decorators import performance_timer_decorator
from pre_processing.kafka.admin import AdminClient
from pre_processing.kafka.data_pipeline import DataPipeline
from pre_processing.kafka.producer import AIOProducer, AvroService
from pre_processing.utils.loader import get_kafka_config
from pre_processing.utils.logger import setup_logs

logger = logging.getLogger("pre_processing")


def load_data(filename: str) -> pd.DataFrame:
    """
    Load data from csv file
    """
    data = pd.read_csv(
        f"{DATA_PATH}/{filename}.csv", dtype=StockChartType, parse_dates=["date"]
    )
    return data


def group_by_ticker(data: pd.DataFrame) -> Dict[str, pd.DataFrame]:
    """
    Group data by ticker
    """
    grouped = data.groupby("ticker")
    return {cast(str, ticker): group for ticker, group in grouped}


@performance_timer_decorator(["day"])
def extract_day(day: str) -> None:
    exclude_list = [
        "GOOG",
        "VICI",
        "PTC",
        "AVGO",
        "WELL",
        "CEG",
        "CZR",
        "NOW",
        "FRC",
        "FLT",
        "WBD",
    ]
    result = gather_data("sp500", exclude_list, day)
    result = pd.concat(result)
    save_date_data(result, day)


def verify_day_extracted(day: str) -> bool:
    """
    Verify that the day has been extracted
    """
    try:
        load_data(f"chart_{day}")
        return True
    except FileNotFoundError:
        return False


def main(
    day: str,
    skip_topic_creation: bool,
    skip_schema_creation: bool,
) -> None:
    """
    Main function to feed kafka with data from 2022-04-05
    """
    setup_logs("pre_processing", level=logging.DEBUG)

    if not verify_day_extracted(day):
        extract_day(day=day)
    else:
        logger.info("Skipping day extraction")

    data = load_data(f"chart_{day}")
    data = preprocess_data(data)
    grouped = group_by_ticker(data)

    for ticker, df in grouped.items():
        grouped[ticker] = bound_dataframe(df)
        grouped[ticker] = remove_ticker(grouped[ticker])

    configs: dict[str, str] = {
        "bootstrap.servers": get_kafka_config()["bootstrap.servers"]
    }

    symbol_topic_prefix: str = get_kafka_config()["common"]["symbol-topic-prefix"]

    data_pipeline = DataPipeline(configs, interval_seconds=3)
    data_pipeline.prepare(grouped, consumer_num_processes=8, producer_num_processes=4)

    # Create topics for each ticker
    if not skip_topic_creation:
        admin_client = AdminClient(configs)
        topics_list = [symbol_topic_prefix + ticker for ticker in grouped.keys()]
        admin_client.delete_topics(topics_list)
        admin_client.create_topics(topics_list)
        if not skip_schema_creation:
            schema_str = AvroService.get_schema_from_file(
                AvroService.SCHEMA_URL, AvroService.SCHEMA_FILE
            )[1]
            try:
                for topic in topics_list:
                    AvroService().update_schema(
                        AvroService.SCHEMA_URL, f"{topic}-value", schema_str
                    )
            except SchemaRegistryError:
                for topic in topics_list:
                    AvroService().register_schema(
                        AvroService.SCHEMA_URL, f"{topic}-value", schema_str
                    )
    else:
        logger.info("Skipping topic creation")

    # Produce an init message for each ticker
    # Could take a while to index all the topics
    producer = AIOProducer(configs)

    data_pipeline.start_producers()
    producer.flush()
    logger.info("Ready to send data")

    data_pipeline.run()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Customize parameters")

    parser.add_argument("--day", default="2022-04-05", help="Specify the day")
    parser.add_argument(
        "--skip-topic-creation", action="store_true", help="Skip topic creation"
    )
    parser.add_argument(
        "--skip-schema-creation", action="store_true", help="Skip schema creation"
    )

    args = parser.parse_args()

    main(
        args.day,
        args.skip_topic_creation,
        args.skip_schema_creation,
    )
