from typing import Generator

import pandas as pd

from pre_processing.data_completion.generation import complete_data


def split_by_hour(data: pd.DataFrame) -> list[pd.DataFrame]:
    """
    Split data by hour constraint to one day
    """
    if data.index[0].date() != data.index[-1].date():
        raise ValueError("Dataframe is not reduced to a single day")

    grouped = data.groupby(data.index.hour)  # type: ignore
    data_by_hour = [group for _, group in grouped]
    return data_by_hour


def complete_a_day_hour_by_hour(
    df: pd.DataFrame, interval_seconds: int = 15
) -> Generator[pd.DataFrame, None, None]:
    """
    Complete a day in a DataFrame by adding new data points between existing data points.

    This function is a generator, it yields a DataFrame for each hour of the day.
    """
    if df.index[0].date() != df.index[-1].date():
        raise ValueError("Dataframe is not reduced to a single day")

    day_splited = split_by_hour(df)

    for index in range(len(day_splited) - 1):
        full_data = day_splited[index]
        if index != len(day_splited) - 1:
            first_data_of_next_segment = day_splited[index + 1].iloc[[0]]
            full_data = pd.concat([full_data, first_data_of_next_segment])

        completed_df = complete_data(full_data, interval_seconds=interval_seconds)
        # Remove last row of the completed_df, because it is the first row of the next segment
        # only used to generate interpolated data
        if index != len(day_splited) - 1:
            completed_df = completed_df.iloc[:-1]
        yield completed_df
