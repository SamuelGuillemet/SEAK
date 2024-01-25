import logging

from pre_processing.utils.logger import setup_logs


def test_logger():
    # Test the logger
    setup_logs("client", level=logging.DEBUG)
    assert True
