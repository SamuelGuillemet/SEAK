import datetime

import pandas as pd
from pre_processing.data_completion.generation import complete_data


def extract_a_day(df: pd.DataFrame, day: datetime.date) -> pd.DataFrame:
    """
    Use index which is a timestamp to extract a day from a DataFrame

    Args:
        df (pd.DataFrame): The DataFrame to extract a day from.
        day (datetime.date): The day to extract.

    Returns:
        pd.DataFrame: The extracted day.
    """
    return df[df.index.date == day]  # type: ignore


def split_by_day(data: pd.DataFrame) -> list[pd.DataFrame]:
    """
    Split data by day
    """
    grouped = data.groupby(data.index.date)  # type: ignore
    data_by_day = [group for _, group in grouped]
    return data_by_day


def complete_a_day(
    df: pd.DataFrame, day: datetime.date, interval: int = 15
) -> pd.DataFrame:
    """
    Complete a day in a DataFrame by adding new data points between existing data points.
    """
    day_df = extract_a_day(df, day)

    if len(day_df) == 0:
        raise ValueError("Day is not in DataFrame")

    completed_df = complete_data(day_df, interval_seconds=interval)

    return completed_df
