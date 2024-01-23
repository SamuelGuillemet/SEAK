import numpy as np
import pandas as pd


def generate_segments(base_number: int, num_segments: int):
    """
    Generate a list of random integers that sum to a specified number.

    Args:
        base_number (int): The number to sum to.
        num_segments (int): The number of segments to generate.

    Returns:
        List[int]: A list of integers that sum to base_number.
    """
    if num_segments == 1:
        return [base_number]

    variation_percentage = 1 / num_segments

    # Calculate the maximum and minimum values for the segments
    max_segment = int(base_number / (num_segments * (1 - variation_percentage))) + 1
    min_segment = int(base_number / (num_segments * (1 + variation_percentage))) - 1

    # Generate random segments using NumPy
    segments = np.random.randint(min_segment, max_segment, size=num_segments)

    # Adjust the last segment to ensure that the sum is equal to base_number
    segments[-1] = base_number - sum(segments[:-1])

    # Set as integers
    segments = segments.astype(int)

    # If any segment is negative, we return a list of equal segments
    if (segments < 0).any():
        return [base_number // num_segments] * num_segments

    return segments


def fast_round(number: float, decimals: int) -> float:
    """
    Round a number to a specified number of decimal places.

    Args:
        number (float): The number to round.
        decimals (int): The number of decimal places to round to.

    Returns:
        float: The rounded number.
    """
    return int(number * 10**decimals) / 10**decimals


def preprocess_data(data: pd.DataFrame, parse_dates: bool = False) -> pd.DataFrame:
    """
    Preprocess data by doing the following:
        - Drop useless columns
        - Convert date column to datetime object if parse_dates is True
        - Set date column as index
        - Sort data by date
        - Drop duplicates
        - Drop rows with NaN values

    Args:
        data (pd.DataFrame): The data to preprocess
        parse_dates (bool): Whether to parse the date column as a datetime object

    Returns:
        pd.DataFrame: The preprocessed data
    """
    # Drop useless columns
    if "X" in data.columns:
        data.drop(columns=["X"], inplace=True)

    # Convert date column to datetime object
    if parse_dates:
        data["date"] = pd.to_datetime(data["date"], format="%Y-%m-%d %H:%M")

    # Set date column as index
    data.set_index("date", inplace=True)
    # Sort data by date
    data.sort_index(inplace=True)
    # Drop duplicates
    data.drop_duplicates(inplace=True)
    # Drop rows with NaN values
    data.dropna(inplace=True)
    return data


def add_ticker(data: pd.DataFrame, ticker: str) -> pd.DataFrame:
    """
    Add ticker column

    Args:
        data (pd.DataFrame): The data to add the ticker column to
        ticker (str): The ticker to add

    Returns:
        pd.DataFrame: The data with the ticker column added
    """
    data["ticker"] = ticker
    return data


def remove_ticker(data: pd.DataFrame) -> pd.DataFrame:
    """
    Remove ticker column

    Args:
        data (pd.DataFrame): The data to remove the ticker column from

    Returns:
        pd.DataFrame: The data with the ticker column removed
    """
    if "ticker" in data.columns:
        data.drop(columns=["ticker"], inplace=True)
    return data


def bound_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """
    Bound a DataFrame by adding the first and last point of the day
    if they are missing

    Args:
        data (pd.DataFrame): The DataFrame to bound

    Returns:
        pd.DataFrame: The bounded DataFrame
    """
    # If there is no point for the opening hour, ie 9:30 we add it as the same as the first point
    if df.index[0].hour != 9 or df.index[0].minute != 30:
        df = pd.concat(
            [
                pd.DataFrame(
                    [df.iloc[0].to_dict()],
                    index=pd.DatetimeIndex(
                        [df.index[0].replace(hour=9, minute=30)], name="date"
                    ),
                ),
                df,
            ]
        )

    # If there is no point for the closing hour, ie 16:00 we add it as the same as the last point
    if df.index[-1].hour != 16 or df.index[-1].minute != 0:
        df = pd.concat(
            [
                df,
                pd.DataFrame(
                    [df.iloc[-1].to_dict()],
                    index=pd.DatetimeIndex(
                        [df.index[-1].replace(hour=16, minute=0)], name="date"
                    ),
                ),
            ]
        )

    return df
