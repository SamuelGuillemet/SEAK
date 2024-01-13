import logging
from copy import deepcopy
from time import sleep

from broker_quickfix_client.application import ClientApplication
from broker_quickfix_client.handlers.execution_report import ExecutionReportHandler
from broker_quickfix_client.wrappers.enums import SideEnum
from broker_quickfix_client.wrappers.execution_report import (
    AcceptedOrderExecutionReport,
    CanceledOrderExecutionReport,
    ReplacedOrderExecutionReport,
)
from broker_quickfix_client.wrappers.new_order_single import NewOrderSingle
from broker_quickfix_client.wrappers.order import Order
from broker_quickfix_client.wrappers.order_cancel_replace_request import (
    OrderCancelReplaceRequest,
)
from broker_quickfix_client.wrappers.order_cancel_request import OrderCancelRequest

logger = logging.getLogger("client")


def order_book_test(application: ClientApplication):
    order_map: dict[int, Order] = {}

    def order_filled_callback(report):
        logger.info(f"Filled: {report}")

    def order_rejected_callback(report):
        logger.warning(f"Rejected: {report}")
        del order_map[report.client_order_id]

    def order_accepted_callback(report: AcceptedOrderExecutionReport):
        logger.info(f"Accepted: {report}")
        order_map[report.client_order_id].order_id = report.order_id

    def order_canceled_callback(report: CanceledOrderExecutionReport):
        logger.info(f"Canceled: {report}")
        del order_map[report.original_client_order_id]

    def order_replaced_callback(report: ReplacedOrderExecutionReport):
        logger.info(f"Replaced: {report}")
        order_map[report.client_order_id] = deepcopy(
            order_map[report.original_client_order_id]
        )
        order_map[report.client_order_id].price = report.price
        order_map[report.client_order_id].quantity = report.leaves_quantity
        order_map[report.client_order_id].client_order_id = report.client_order_id

    execution_handler = ExecutionReportHandler(
        order_filled_callback=order_filled_callback,
        order_rejected_callback=order_rejected_callback,
        order_accepted_callback=order_accepted_callback,
        order_canceled_callback=order_canceled_callback,
        order_replaced_callback=order_replaced_callback,
    )
    application.set_execution_report_handler(execution_handler)

    order = NewOrderSingle.new_limit_order(1, SideEnum.BUY, 1, "ACGL", 40.4)
    application.send(order)
    order_map[1] = order.get_order()
    while order_map[1].order_id is None:
        sleep(0.1)
    replaced_order = OrderCancelReplaceRequest.new_replace_order(
        2, order_map[1], 40.5, 2
    )
    application.send(replaced_order)
    # Wait for the order to be replaced at index 2
    while order_map.get(2) is None:
        sleep(0.1)
    canceled_order = OrderCancelRequest.new_cancel_order(3, order_map[2])
    application.send(canceled_order)
    while order_map.get(2) is not None:
        sleep(0.1)

    logger.info("Done")
