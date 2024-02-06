import os

import mplfinance as mpf
import pandas as pd
from broker_quickfix_client.wrappers.market_data import MarketDataResponse
from mplfinance.dates import date2num


def create_candlestick_chart(market_data_response: MarketDataResponse):
    symbol = market_data_response.symbol

    # Extract relevant data for the first symbol
    entries = market_data_response.market_data.get(symbol, [])

    # Prepare candlestick data
    ohlc_data = []
    for entry in entries:
        ohlc_data.append(
            (
                date2num(entry.md_entry_time),
                entry.md_entry_px.open,
                entry.md_entry_px.high,
                entry.md_entry_px.low,
                entry.md_entry_px.close,
            )
        )

    # Create a simple candlestick chart
    df = mpf.DataFrame(ohlc_data, columns=["Date", "Open", "High", "Low", "Close"])
    df["Date"] = pd.to_datetime(df["Date"], unit="s")

    mpf.plot(
        df,
        type="candle",
        style="yahoo",
        title=f"Candlestick Chart - {symbol}",
        ylabel="Price",
        savefig=os.path.join("charts", f"{symbol}.png"),
    )
