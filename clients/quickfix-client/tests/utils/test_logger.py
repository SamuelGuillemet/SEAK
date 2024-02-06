from broker_quickfix_client.utils.logger import setup_logs


def test_logger():
    # Test the logger
    setup_logs("app")
    assert True
