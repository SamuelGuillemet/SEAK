"""
Main file to feed kafka with data from 2022-04-05
"""

import logging
from typing import Dict, cast

import pandas as pd
from pfe_preprocessing.constant import DATA_PATH, StockChartType
from pfe_preprocessing.data_agregation.helpers import save_date_data
from pfe_preprocessing.data_agregation.reader import gather_data
from pfe_preprocessing.data_completion.helpers import (
    bound_dataframe,
    preprocess_data,
    remove_ticker,
)
from pfe_preprocessing.decorators import performance_timer_decorator
from pfe_preprocessing.kafka.admin import AdminClient
from pfe_preprocessing.kafka.data_pipeline import DataPipeline
from pfe_preprocessing.kafka.producer import AIOProducer
from pfe_preprocessing.utils.logger import setup_logs

logger = logging.getLogger("pfe_preprocessing")


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


def main() -> None:
    """
    Main function to feed kafka with data from 2022-04-05
    """
    setup_logs("pfe_preprocessing")
    DAY = "2022-04-05"
    SKIP_DAY_EXTRACTION = True
    SKIP_TOPIC_CREATION = True

    if not SKIP_DAY_EXTRACTION:
        extract_day(day=DAY)
    else:
        logger.info("Skipping day extraction")

    data = load_data(f"chart_{DAY}")
    data = preprocess_data(data)
    grouped = group_by_ticker(data)

    for ticker, df in grouped.items():
        grouped[ticker] = bound_dataframe(df)
        grouped[ticker] = remove_ticker(grouped[ticker])

    configs = {"bootstrap.servers": "localhost:9092"}

    data_pipeline = DataPipeline(configs, interval_seconds=1)
    data_pipeline.prepare(grouped, consumer_num_processes=8, producer_num_processes=2)

    # Create topics for each ticker
    if not SKIP_TOPIC_CREATION:
        admin_client = AdminClient(configs)
        admin_client.delete_topics(list(grouped.keys()))
        admin_client.create_topics(list(grouped.keys()))
    else:
        logger.info("Skipping topic creation")

    # Produce an init message for each ticker
    # Could take a while to index all the topics
    producer = AIOProducer(configs)

    for ticker, _ in list(grouped.items()):
        producer.produce(ticker, "init", "init")

    data_pipeline.start_producers()
    producer.flush()
    logger.info("Ready to send data")

    data_pipeline.run()


if __name__ == "__main__":
    main()
