import pytest
from broker_quickfix_client.wrappers.enums import MarketDataEntryTypeEnum
from broker_quickfix_client.wrappers.market_data import (
    MarketDataDetails,
    MarketDataResponse,
)

from quickfix_client_gui.interface.candlestick_chart import create_candlestick_chart


@pytest.fixture
def sample_data():
    market_data = {
        1: [
            MarketDataDetails(MarketDataEntryTypeEnum.HIGH, 155.0),
            MarketDataDetails(MarketDataEntryTypeEnum.OPEN, 150.0),
            MarketDataDetails(MarketDataEntryTypeEnum.LOW, 145.0),
            MarketDataDetails(MarketDataEntryTypeEnum.CLOSE, 152.5),
        ],
        2: [
            MarketDataDetails(MarketDataEntryTypeEnum.OPEN, 160.0),
            MarketDataDetails(MarketDataEntryTypeEnum.HIGH, 165.0),
            MarketDataDetails(MarketDataEntryTypeEnum.LOW, 155.0),
            MarketDataDetails(MarketDataEntryTypeEnum.CLOSE, 162.5),
        ],
        3: [
            MarketDataDetails(MarketDataEntryTypeEnum.OPEN, 170.0),
            MarketDataDetails(MarketDataEntryTypeEnum.HIGH, 175.0),
            MarketDataDetails(MarketDataEntryTypeEnum.LOW, 165.0),
            MarketDataDetails(MarketDataEntryTypeEnum.CLOSE, 172.5),
        ],
        4: [
            MarketDataDetails(MarketDataEntryTypeEnum.OPEN, 180.0),
            MarketDataDetails(MarketDataEntryTypeEnum.HIGH, 185.0),
            MarketDataDetails(MarketDataEntryTypeEnum.LOW, 175.0),
            MarketDataDetails(MarketDataEntryTypeEnum.CLOSE, 182.5),
        ],
        5: [
            MarketDataDetails(MarketDataEntryTypeEnum.OPEN, 190.0),
            MarketDataDetails(MarketDataEntryTypeEnum.HIGH, 195.0),
            MarketDataDetails(MarketDataEntryTypeEnum.LOW, 185.0),
            MarketDataDetails(MarketDataEntryTypeEnum.CLOSE, 192.5),
        ],
    }

    return MarketDataResponse(md_req_id=1, symbol="AAPL", market_data=market_data)


def test_candlestick_chart(sample_data):
    create_candlestick_chart(sample_data)
