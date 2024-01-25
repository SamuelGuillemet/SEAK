import logging
import multiprocessing
import signal
import sys
import time
from multiprocessing.managers import DictProxy
from multiprocessing.synchronize import Event
from typing import Dict, Generator, List, cast

import pandas as pd
from pre_processing.constant import MARKET_DATA_PARTIONS
from pre_processing.dataframe_extraction.hour import complete_a_day_hour_by_hour
from pre_processing.decorators import performance_timer_decorator
from pre_processing.kafka.producer import AIOProducer
from pre_processing.utils.loader import get_kafka_config

logger = logging.getLogger("pre_processing.kafka.data_pipeline")


class DataPipeline:
    producer_processes: List[multiprocessing.Process]
    consumer_processes: List[multiprocessing.Process]
    consumer_events: List[Event]

    number_of_tickers = 0

    # Boolean variables to check if the data and processes are ready
    prepared: bool = False
    ready: bool = False

    # Boolean variable to exit the program
    exited: bool = False

    # Topic prefix
    topic_prefix: str = get_kafka_config()["common"]["symbol-topic-prefix"]

    def __init__(self, configs: dict[str, str], interval_seconds: int = 60):
        self.data_dict: DictProxy[str, pd.DataFrame] = multiprocessing.Manager().dict()
        self.ticker_list: List[str] = []
        self.main_sync_event = multiprocessing.Event()
        self.configs = configs
        self.interval_seconds = interval_seconds

        signal.signal(signal.SIGINT, self.sigint_handler)

        self.producer_processes = []
        self.consumer_processes = []
        self.consumer_events = []

    def sigint_handler(self, signal, frame):
        """
        This function is called when a SIGINT signal is received.
        It terminates all the processes.
        """
        print("SIGINT received, shutting down")
        for process in self.producer_processes:
            process.terminate()
        for process in self.consumer_processes:
            process.terminate()

        self.exited = True

        sys.exit(0)

    def get_own_ticker_list(self, process_num: int, entries_per_process: int):
        """
        Get the ticker list for the current process
        """
        start_index = process_num * entries_per_process
        end_index = (process_num + 1) * entries_per_process

        return self.ticker_list[start_index:end_index]

    def producer(
        self,
        generator_dict: dict[str, Generator[pd.DataFrame, None, None]],
    ) -> None:
        """
        This function is a producer, it fills the data_dict with data from the generators.

        The data_dict is a shared dictionary between all the processes.
        The data_dict is filled using segments, each segment is a DataFrame
        containing data for a single hour of a single day.

        The data_dict is filled with the following format:
        {
            "ticker_name-segment": pd.DataFrame
        }

        Args:
            generator_dict (dict[str, Generator[pd.DataFrame, None, None]]): The dictionary of generators.
        """
        ticker_seg_count_dict = {ticker: 0 for ticker in generator_dict.keys()}
        while True:
            for ticker, generator in generator_dict.items():
                entry_name = f"{ticker}-{ticker_seg_count_dict[ticker]}"
                try:
                    self.data_dict[entry_name] = next(generator)

                    ticker_seg_count_dict[ticker] += 1
                except StopIteration:
                    pass

    def recover_data(self, ticker: str, iteration: int, segment: int):
        """
        Recover data from the data_dict.

        The data_dict is filled with the following format:
        {
            "ticker_name-segment": pd.DataFrame
        }

        The function returns None if the segment is not in the data_dict.

        Args:
            ticker (str): The ticker name.
            iteration (int): The current iteration, ie the current row index.
            segment (int): The current segment, ie the current hour index.

        Returns:
            tuple[tuple[str, str], tuple[int, int]]: The data and the new iteration and segment.
        """
        entry_name = f"{ticker}-{segment}"
        if entry_name in self.data_dict.keys():
            hour_df = self.data_dict[f"{ticker}-{segment}"]

            value = hour_df.iloc[iteration].to_dict()
            key = cast(pd.Timestamp, hour_df.iloc[iteration].name).strftime(
                "%Y-%m-%d %H:%M:%S"
            )

            new_iteration = iteration + 1
            new_segment = segment
            if new_iteration >= len(hour_df):
                del self.data_dict[entry_name]
                new_iteration = 0
                new_segment += 1

            return (key, value), (new_iteration, new_segment)

        return None, (iteration, segment)

    def consumer(self, process_num: int, entries_per_process: int):
        """
        This function is a consumer, it consumes data from the data_dict and sends it to Kafka.

        The data_dict is a shared dictionary between all the processes.

        The data_dict is filled with the following format:
        {
            "ticker_name-segment": pd.DataFrame
        }

        Args:
            process_num (int): The number of the process.
            entries_per_process (int): The number of entries per process.
        """
        kafka_producer = AIOProducer(self.configs)

        # Extract data for the current process
        process_data = self.get_own_ticker_list(process_num, entries_per_process)
        seg_iter_dict: dict[str, tuple[int, int]] = {
            ticker: (0, 0) for ticker in process_data
        }
        counter = 0
        while True:
            # Wait for the synchronization event to be set
            self.main_sync_event.wait()

            start_time = time.perf_counter()

            partition_number = (process_num + counter) % MARKET_DATA_PARTIONS
            counter += 1

            last_key: str = ""
            for index, ticker_name in enumerate(process_data):
                data, seg_iter_dict[ticker_name] = self.recover_data(
                    ticker_name, *seg_iter_dict[ticker_name]
                )
                if data is not None:
                    key, value = data
                    if index == 0:
                        last_key = key
                    kafka_producer.produce(
                        self.topic_prefix + ticker_name, value, key, partition_number
                    )
                else:
                    logger.warning(f"Data is None for {ticker_name}")

            kafka_producer.flush()
            logger.debug(f"{last_key} for process {process_num}")

            end_time = time.perf_counter()
            execution_time = end_time - start_time

            # Check if the execution time is greater than the interval time
            if execution_time > self.interval_seconds:
                logger.warning(
                    f"Execution time is greater than interval time: {execution_time} > {self.interval_seconds}"
                )

            # Clear the synchronization event
            self.main_sync_event.clear()
            # Sync the event related to the current process
            self.consumer_events[process_num].set()

    def prepare(
        self,
        data: Dict[str, pd.DataFrame],
        consumer_num_processes: int = 3,
        producer_num_processes: int = 1,
    ) -> None:
        """
        Prepare data for the pipeline

        This function creates the producer and consumer processes, the generators and the data_dict.
        """
        self.ticker_list = list(data.keys())
        self.number_of_tickers = len(self.ticker_list)

        # Create the producer process
        self.producer_processes = []
        entries_per_process = (self.number_of_tickers // producer_num_processes) + 1
        for process_num in range(producer_num_processes):
            ticker_concerned = self.get_own_ticker_list(
                process_num, entries_per_process
            )
            generators = {
                ticker: complete_a_day_hour_by_hour(
                    data[ticker], interval_seconds=self.interval_seconds
                )
                for ticker in ticker_concerned
            }
            process = multiprocessing.Process(
                target=self.producer,
                args=(generators,),
                name=f"producer-{process_num}",
            )
            self.producer_processes.append(process)

        # Create the consumer processes
        self.consumer_processes = []
        entries_per_process = (self.number_of_tickers // consumer_num_processes) + 1
        for process_num in range(consumer_num_processes):
            process = multiprocessing.Process(
                target=self.consumer,
                args=(process_num, entries_per_process),
                name=f"consumer-{process_num}",
            )
            self.consumer_processes.append(process)

            event = multiprocessing.Event()
            self.consumer_events.append(event)

        self.prepared = True
        logger.info("Data prepared and processes created")

    @performance_timer_decorator()
    def start_producers(self):
        """
        Start the producer processes and wait for the data_dict to be filled.
        """
        if not self.prepared:
            raise RuntimeError("Data and processes not prepared")

        for process in self.producer_processes:
            process.start()

        while True:
            count = self.count_filled_data()
            if count == self.number_of_tickers:
                break

            logger.info(
                f"Waiting for data_dict to be filled: {count}/{self.number_of_tickers}"
            )
            time.sleep(1)

        self.ready = True
        logger.info("Producers started and initial data filled")

    def count_filled_data(self):
        """
        Count the number of tickers with initial data in the data_dict
        """
        return sum(1 for ticker in self.ticker_list if f"{ticker}-0" in self.data_dict)

    def run(self):
        """
        Start the consumer processes and start the pipeline.
        """
        if not self.ready:
            raise RuntimeError("Data and processes not ready")

        for process in self.consumer_processes:
            process.start()

        overflow_exec_time = 0
        while not self.exited:
            start_time = time.perf_counter()
            logger.debug("-------------------------------")

            # Set the synchronization event
            self.main_sync_event.set()

            # Here the different processes are running in parallel

            time.sleep(max(0, self.interval_seconds - overflow_exec_time))

            # Wait for all processes to finish
            for event in self.consumer_events:
                event.wait()
                event.clear()

            end_time = time.perf_counter()

            # Compute the execution time to reduce the sleep time for the next iteration in case
            # the execution time is greater than the interval time
            overflow_exec_time = max(0, end_time - start_time - self.interval_seconds)

            # We tolerate a 5% error before warning
            if overflow_exec_time > 0.05 * self.interval_seconds:
                logger.warning(
                    (
                        "Global sending time is greater than interval time:"
                        f"{end_time - start_time} > {self.interval_seconds}"
                    )
                )
            else:
                logger.debug(
                    (
                        "Global sending time is lower than interval time:"
                        f"{end_time - start_time} < {self.interval_seconds}"
                    )
                )
