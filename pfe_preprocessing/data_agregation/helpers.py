import pandas as pd

from pfe_preprocessing.constant import CSV_COLUMNS, DATA_PATH
from pfe_preprocessing.data_completion.helpers import (
    add_ticker,
    bound_dataframe,
    preprocess_data,
)


def parse_csv_line(line: str) -> list[str]:
    """
    Parse a line from a CSV file to a list of strings

    Args:
        line (str): A line from a CSV file

    Returns:
        list[str]: The parsed line
    """
    return line.removesuffix("\n").split(",")


def generate_dataframe(lines: list[str], ticker_name: str) -> pd.DataFrame:
    """
    Generate a DataFrame from a list of lines

    Args:
        lines (list[str]): A list of lines from a CSV file
        ticker_name (str): The name of the ticker

    Returns:
        pd.DataFrame: The DataFrame generated from the list of lines
    """
    data = [parse_csv_line(line) for line in lines]
    df = pd.DataFrame(data, columns=CSV_COLUMNS)
    df = preprocess_data(df, parse_dates=True)
    df = bound_dataframe(df)
    df = add_ticker(df, ticker_name)
    return df


def save_date_data(data: pd.DataFrame, date: str) -> None:
    """
    Save data by day
    """
    if len(data) <= 0:
        return

    data.to_csv(f"{DATA_PATH}/chart_{date}.csv")
