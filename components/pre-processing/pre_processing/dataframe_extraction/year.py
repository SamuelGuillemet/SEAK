import pandas as pd

from pre_processing.data_completion.generation import complete_data
from pre_processing.dataframe_extraction import day


def extract_a_year(df: pd.DataFrame, year: int) -> pd.DataFrame:
    """
    Use index which is a timestamp to extract a year from a DataFrame

    Args:
        df (pd.DataFrame): The DataFrame to extract a year from.
        year (int): The year to extract.

    Returns:
        pd.DataFrame: The extracted year.
    """
    return df[df.index.year == year]  # type: ignore


def split_by_year(data: pd.DataFrame) -> list[pd.DataFrame]:
    """
    Split data by year
    """
    grouped = data.groupby(data.index.year)  # type: ignore
    data_by_year = [group for _, group in grouped]
    return data_by_year


def complete_a_year(df: pd.DataFrame, year: int, interval: int = 15) -> pd.DataFrame:
    """
    Complete a year in a DataFrame by adding new data points between existing data points.
    """
    year_df = extract_a_year(df, year)

    if len(year_df) == 0:
        raise ValueError("Year is not in DataFrame")

    completed_df = pd.concat(
        [
            complete_data(data, interval_seconds=interval)
            for data in day.split_by_day(year_df)
        ]
    )

    return completed_df
