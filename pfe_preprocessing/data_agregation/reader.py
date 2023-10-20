import gc
import re
import zipfile
from typing import IO, Iterator, List, Tuple

import pandas as pd
from pfe_preprocessing.constant import DATA_PATH, TARGET_DATE_REGEX
from pfe_preprocessing.data_agregation.find_date import extract_full_day_from_file
from pfe_preprocessing.data_agregation.helpers import generate_dataframe
from pfe_preprocessing.decorators import performance_timer_decorator


def zipfile_reader(
    zip_filename: str, exclude_list: list[str]
) -> Iterator[Tuple[IO[bytes], str]]:
    if zip_filename.endswith(".zip"):
        zip_filename = zip_filename[:-4]

    with zipfile.ZipFile(f"{DATA_PATH}/{zip_filename}.zip") as zip_file:
        counter = 0
        for filename in zip_file.namelist():
            if not filename.endswith(".csv"):
                continue
            ticker_name = filename.split("_")[0].split("/")[1]
            if ticker_name in exclude_list:
                continue
            counter += 1

            with zip_file.open(filename, "r") as file:
                yield file, ticker_name

            if counter % 50 == 0:
                gc.collect()


@performance_timer_decorator()
def gather_data(
    zip_filename: str, exclude_list: list[str], target_date: str
) -> list[pd.DataFrame]:
    """
    Gather data for a specific date from a zip file

    Args:
        zip_filename (str): The zip file to gather the data from
        exclude_list (list[str]): A list of tickers to exclude
        target_date (str): The date to gather the data for, in the format "YYYY-MM-DD"

    Returns:
        list[pd.DataFrame]: A list of DataFrames containing the data for the date
    """
    if not re.match(TARGET_DATE_REGEX, target_date):
        raise ValueError("target_date must be in the format YYYY-MM-DD")

    dataframes: List[pd.DataFrame] = []
    for file, ticker_name in zipfile_reader(zip_filename, exclude_list):
        day_lines = extract_full_day_from_file(file, target_date)
        dataframes.append(generate_dataframe(day_lines, ticker_name))

    return dataframes
