from typing import List, Tuple, cast

import numpy as np
import pandas as pd

from pre_processing.data_completion.helpers import fast_round, generate_segments
from pre_processing.decorators import performance_timer_decorator


def generate_new_data(
    start_row: pd.Series,
    end_row: pd.Series,
    interval_seconds: float,
    variation_coef: float = 1,
) -> Tuple[List[Tuple[pd.Timestamp, dict]], int]:
    """
    Generate new data points between two rows of data.

    Args:
        start_date (pd.Timestamp): The timestamp of the first row.
        start_row (pd.Series): The first row of data.
        end_date (pd.Timestamp): The timestamp of the last row.
        end_row (pd.Series): The last row of data.
        interval_seconds (int): The number of seconds between each data point.

    Returns:
        Tuple[List[Tuple[pd.Timestamp, dict]], int]: A tuple containing the new data points and
            the volume of the first row.
    """
    start_date = cast(pd.Timestamp, start_row.name)
    end_date = cast(pd.Timestamp, end_row.name)
    # Calculate the number of intervals
    num_intervals = int((end_date - start_date).total_seconds() / interval_seconds)

    open_coef = (end_row["open"] - start_row["open"]) / num_intervals
    close_coef = (end_row["close"] - start_row["close"]) / num_intervals
    high_coef = (end_row["high"] - start_row["high"]) / num_intervals
    low_coef = (end_row["low"] - start_row["low"]) / num_intervals

    randoms = np.random.uniform(
        -variation_coef, variation_coef, size=(num_intervals - 1, 4)
    )

    interval_indices = np.arange(1, num_intervals)

    # Calculate the times for the new data points
    new_dates = start_date + pd.to_timedelta(
        interval_indices * interval_seconds, unit="s"
    )

    # Interpolate open and close values
    open_vals = start_row["open"] + (interval_indices + randoms[:, 0]) * open_coef
    close_vals = start_row["close"] + (interval_indices + randoms[:, 1]) * close_coef
    high_vals = start_row["high"] + (interval_indices + randoms[:, 2]) * high_coef
    low_vals = start_row["low"] + (interval_indices - randoms[:, 3]) * low_coef

    # Calculate high and low values with adjustments
    high_vals_adjusted = np.maximum.reduce([open_vals, close_vals, high_vals]) + 0.0001
    low_vals_adjusted = np.minimum.reduce([open_vals, close_vals, low_vals]) - 0.0001

    # Calculate volumes with adjustments
    volume_vals = generate_segments(start_row["volume"], num_intervals)

    intermediate_data = [
        (
            date,
            {
                "open": fast_round(open_val, 4),
                "high": fast_round(high_val, 4),
                "low": fast_round(low_val, 4),
                "close": fast_round(close_val, 4),
                "volume": volume_val,
            },
        )
        for date, open_val, high_val, low_val, close_val, volume_val in zip(
            new_dates,
            open_vals,
            high_vals_adjusted,
            low_vals_adjusted,
            close_vals,
            volume_vals,
        )
    ]

    start_row_volume = volume_vals[-1]

    return (intermediate_data, start_row_volume)


@performance_timer_decorator(disable=True)
def complete_data(df: pd.DataFrame, interval_seconds: float) -> pd.DataFrame:
    """
    Complete the data in a DataFrame by adding new data points between existing data points.

    The new data points are generated using linear interpolation between the existing data points,
    and adding a random value between -1 and 1 multiplied by the variation coefficient.

    The data is assumed to be reduced to a single day.

    Args:
        df (pd.DataFrame): The DataFrame to complete.
        interval_seconds (int): The number of seconds between each data point.

    Returns:
        pd.DataFrame: The completed DataFrame.
    """
    # Verify that the dataframe is reduced to a single day
    if df.index[0].date() != df.index[-1].date():
        raise ValueError("Dataframe is not reduced to a single day")

    # Create an empty list to store the new data points
    intermediate_data = []

    # Iterate over the rows in the DataFrame
    for start_date, end_date in zip(df.index[:-1], df.index[1:]):
        start_row = df.loc[start_date]
        end_row = df.loc[end_date]

        # Generate new data points between the current row and the next row
        generated_data, start_row_volume = generate_new_data(
            start_row=start_row, end_row=end_row, interval_seconds=interval_seconds
        )

        intermediate_data.extend(generated_data)
        df.loc[start_date, "volume"] = start_row_volume

    # Add the new data points to the DataFrame
    df = pd.concat(
        [
            df,
            pd.DataFrame(
                [data for _, data in intermediate_data],
                index=pd.DatetimeIndex(
                    [data for data, _ in intermediate_data], name="date"
                ),
            ),
        ]
    )
    df = df.sort_index()

    return df
