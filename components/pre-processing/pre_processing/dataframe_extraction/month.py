import pandas as pd

from pre_processing.data_completion.generation import complete_data
from pre_processing.dataframe_extraction import day


def extract_a_month(df: pd.DataFrame, year: int, month: int) -> pd.DataFrame:
    """
    Use index which is a timestamp to extract a month from a DataFrame

    Args:
        df (pd.DataFrame): The DataFrame to extract a month from.
        month (int): The month to extract.
        year (int): The year to extract.

    Returns:
        pd.DataFrame: The extracted month.
    """
    year_df = df[df.index.year == year]  # type: ignore
    month_df = year_df[year_df.index.month == month]  # type: ignore
    return month_df


def split_by_month(data: pd.DataFrame) -> list[pd.DataFrame]:
    """
    Split data by month
    """
    grouped = data.groupby([data.index.month, data.index.year])  # type: ignore
    data_by_month = [group for _, group in grouped]
    return data_by_month


def complete_a_month(
    df: pd.DataFrame, year: int, month: int, interval: int = 15
) -> pd.DataFrame:
    """
    Complete a month in a DataFrame by adding new data points between existing data points.
    """
    month_df = extract_a_month(df, year, month)

    if len(month_df) == 0:
        raise ValueError("Month is not in DataFrame")

    completed_df = pd.concat(
        [
            complete_data(data, interval_seconds=interval)
            for data in day.split_by_day(month_df)
        ]
    )

    return completed_df
