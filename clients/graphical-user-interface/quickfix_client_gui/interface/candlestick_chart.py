import os

import mplfinance as mpf
import pandas as pd
from broker_quickfix_client.wrappers.enums import MarketDataEntryTypeEnum
from broker_quickfix_client.wrappers.market_data import MarketDataResponse


def create_candlestick_chart(market_data_response: MarketDataResponse):
    symbol = market_data_response.symbol

    ohlc_list = []
    for item_id, details_list in market_data_response.market_data.items():
        detail_dict = {
            MarketDataEntryTypeEnum.OPEN: None,
            MarketDataEntryTypeEnum.HIGH: None,
            MarketDataEntryTypeEnum.LOW: None,
            MarketDataEntryTypeEnum.CLOSE: None,
        }
        for detail in details_list:
            detail_dict[
                MarketDataEntryTypeEnum(detail.md_entry_type)
            ] = detail.md_entry_px
        ohlc = (
            detail_dict[MarketDataEntryTypeEnum.OPEN],
            detail_dict[MarketDataEntryTypeEnum.HIGH],
            detail_dict[MarketDataEntryTypeEnum.LOW],
            detail_dict[MarketDataEntryTypeEnum.CLOSE],
        )
        ohlc_list.append(ohlc)

    index_values = pd.date_range(start="today", periods=len(ohlc_list), freq="T")
    df = pd.DataFrame(
        ohlc_list, columns=["Open", "High", "Low", "Close"], index=index_values
    )

    mpf.plot(
        df,
        type="candle",
        style="yahoo",
        title=f"{symbol} stock - last 5 mins",
        ylabel="Price",
        savefig=os.path.join("quickfix_client_gui/charts", f"{symbol}.png"),
        show_nontrading=True,
    )
