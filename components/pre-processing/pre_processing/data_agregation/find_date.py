import io
from typing import IO

from pre_processing.decorators import performance_timer_decorator


def extract_date(line: str) -> str:
    """
    Extract the date from a line in a csv file

    Args:
        line (str): A line in the file

    Returns:
        str: The date in the line as "YYYY-MM-DD"
    """
    return line.split(",")[0].split(" ")[0]


def find_first_date_occurence(lines: list[str], target_date: str) -> int:
    """
    Find the first occurence of a date in a list of lines

    Args:
        lines (list[str]): A list of lines in the file
        target_date (str): The date to find

    Returns:
        int: The index of the first occurence of the date in the list of lines
    """
    # Perform binary search to find the position
    left, right = 0, len(lines) - 1
    while left <= right:
        mid = left + (right - left) // 2
        date_str = extract_date(lines[mid])
        if date_str == target_date:
            return mid

        if date_str < target_date:
            left = mid + 1
        else:
            right = mid - 1

    return -1


@performance_timer_decorator(["target_date"])
def extract_full_day_from_file(file: IO[bytes], target_date: str) -> list[str]:
    """
    Extract the data for a specific date from a file

    Args:
        file (IO[bytes]): The file to extract the data from
        target_date (str): The date to extract

    Returns:
        list[str]: The data for the date
    """
    wrapped_file = io.TextIOWrapper(file, encoding="utf-8")
    lines = wrapped_file.readlines()

    # Find the first occurence of the date
    first_date_occurence = find_first_date_occurence(lines, target_date)

    if first_date_occurence == -1:
        return []

    # Find the last occurence of the date
    start_of_day, end_of_day = -1, -1
    for i in range(first_date_occurence, len(lines)):
        date_str = extract_date(lines[i])
        if date_str != target_date:
            end_of_day = i - 1
            break

    for i in range(first_date_occurence, 0, -1):
        date_str = extract_date(lines[i])
        if date_str != target_date:
            start_of_day = i + 1
            break

    if start_of_day == -1 or end_of_day == -1:
        return []

    return lines[start_of_day:end_of_day]
