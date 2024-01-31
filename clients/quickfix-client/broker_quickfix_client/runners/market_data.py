import logging
from time import sleep

from broker_quickfix_client.application import ClientApplication
from broker_quickfix_client.handlers.market_data_snapshot_full_refresh import (
    MarketDataSnapshotFullRefreshHandler,
)
from broker_quickfix_client.wrappers.enums import MarketDataEntryTypeEnum
from broker_quickfix_client.wrappers.market_data import MarketDataResponse
from broker_quickfix_client.wrappers.market_data_request import MarketDataRequest

logger = logging.getLogger("client")


def market_data_test(application: ClientApplication):
    def market_data_snapshot_full_refresh_callback(
        market_data_response: MarketDataResponse,
    ):
        logger.info(f"Market data for {market_data_response.symbol}:")
        sorted_dict = dict(sorted(market_data_response.market_data.items()))
        for entry in sorted(sorted_dict.values()):
            string = "\t"
            for element in entry:
                string += f"{element.md_entry_type.name}= {element.md_entry_px} "
            logger.info(string)

    market_data_handler = MarketDataSnapshotFullRefreshHandler(
        market_data_snapshot_full_refresh_callback=market_data_snapshot_full_refresh_callback,
    )
    application.set_market_data_snapshot_full_refresh_handler(market_data_handler)

    market_data_request_snapshot = MarketDataRequest.new_snapshot_request(
        1,
        5,
        ["ACGL", "AAPL", "GOOGL"],
        [
            MarketDataEntryTypeEnum.OPEN,
            MarketDataEntryTypeEnum.CLOSE,
            MarketDataEntryTypeEnum.HIGH,
            MarketDataEntryTypeEnum.LOW,
        ],
    )
    application.send(market_data_request_snapshot)
    sleep(5)

    market_data_request_subscribe = MarketDataRequest.new_subscribe_request(
        2,
        ["ACGL", "AAPL", "GOOGL"],
        [
            MarketDataEntryTypeEnum.OPEN,
            MarketDataEntryTypeEnum.CLOSE,
            MarketDataEntryTypeEnum.HIGH,
            MarketDataEntryTypeEnum.LOW,
        ],
    )
    application.send(market_data_request_subscribe)

    sleep(2)

    market_data_request_unsubscribe = MarketDataRequest.new_unsubscribe_request(
        market_data_request_subscribe.get_market_data_request()
    )
    application.send(market_data_request_unsubscribe)

    sleep(5)
