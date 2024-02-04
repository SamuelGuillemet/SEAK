import argparse
import faulthandler
import logging

from broker_quickfix_client.application import setup, start_initiator
from broker_quickfix_client.runners.latency import latency_test
from broker_quickfix_client.runners.market_data import market_data_test
from broker_quickfix_client.runners.order_book import order_book_test

logger = logging.getLogger("client")

faulthandler.enable()


def main(username: str, password: str):
    application, initiator = setup(username, password)
    start_initiator(initiator, application)
    latency_test(username, application)
    order_book_test(application)
    market_data_test(application)
    initiator.stop()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Customize parameters")

    parser.add_argument(
        "--username",
        type=str,
        default="user1",
        help="The username to use to connect to the server",
    )
    parser.add_argument(
        "--password",
        type=str,
        default="password",
        help="The password to use to connect to the server",
    )

    args = parser.parse_args()

    main(args.username, args.password)
