import logging
import time
from typing import List

logger = logging.getLogger("pfe_preprocessing.decorators")


def performance_timer_decorator(context_args: List[str] | None = None, disable=False):
    """
    Decorator to time the execution of a function

    Args:
        context_args (List[str], optional): A list of arguments to print in the context. Defaults to None.
        disable (bool, optional): Disable the decorator. Defaults to False.

    Returns:
        function: The decorated function
    """

    def decorator(func):
        def wrapper(*args, **kwargs):
            start_time = time.perf_counter()
            result = func(*args, **kwargs)
            end_time = time.perf_counter()
            execution_time = end_time - start_time

            if disable:
                return result

            context = ""
            for arg in context_args or []:
                context += str(kwargs[arg]) + " "

            if context is not None:
                logger.debug(
                    f"{context} - {func.__name__} took {execution_time:.6f} seconds to execute"
                )
            else:
                logger.debug(
                    f"{func.__name__} took {execution_time:.6f} seconds to execute"
                )
            return result

        return wrapper

    return decorator
