from dataclasses import dataclass


@dataclass
class OrderCancelReject:
    order_id: int
    client_order_id: int
    original_client_order_id: int
