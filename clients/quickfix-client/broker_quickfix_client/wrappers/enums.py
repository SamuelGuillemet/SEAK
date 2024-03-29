from enum import Enum


class OrderTypeEnum(Enum):
    MARKET = "1"
    LIMIT = "2"
    STOP = "3"
    STOP_LIMIT = "4"


class SideEnum(Enum):
    BUY = "1"
    SELL = "2"


class OrderStatusEnum(Enum):
    NEW = "0"
    PARTIALLY_FILLED = "1"
    FILLED = "2"
    DONE_FOR_DAY = "3"
    CANCELED = "4"
    REPLACED = "5"
    PENDING_CANCEL = "6"
    STOPPED = "7"
    REJECTED = "8"
    SUSPENDED = "9"
    PENDING_NEW = "A"
    CALCULATED = "B"
    EXPIRED = "C"
    ACCEPTED_FOR_BIDDING = "D"
    PENDING_REPLACE = "E"


class ExecTypeEnum(Enum):
    NEW = "0"
    DONE_FOR_DAY = "3"
    CANCELED = "4"
    REPLACED = "5"
    PENDING_CANCEL = "6"
    STOPPED = "7"
    REJECTED = "8"
    SUSPENDED = "9"
    PENDING_NEW = "A"
    CALCULATED = "B"
    EXPIRED = "C"
    RESTATED = "D"
    PENDING_REPLACE = "E"
    TRADE = "F"
    TRADE_CORRECT = "G"
    TRADE_CANCEL = "H"
    ORDER_STATUS = "I"


class OrderRejectReasonEnum(Enum):
    BROKER_CREDIT = "0"
    UNKNOWN_SYMBOL = "1"
    EXCHANGE_CLOSED = "2"
    ORDER_EXCEEDS_LIMIT = "3"
    TOO_LATE_TO_ENTER = "4"
    UNKNOWN_ORDER = "5"
    DUPLICATE_ORDER = "6"
    DUPLICATE_OF_A_VERBALLY_COMMUNICATED_ORDER = "7"
    STALE_ORDER = "8"
    TRADE_ALONG_REQUIRED = "9"
    INVALID_INVESTOR_ID = "10"
    UNSUPPORTED_ORDER_CHARACTERISTIC = "11"
    INCORRECT_QUANTITY = "13"
    INCORRECT_ALLOCATED_QUANTITY = "14"
    UNKNOWN_ACCOUNT = "15"
    OTHER = "99"


class CxlRejResponseToEnum(Enum):
    ORDER_CANCEL_REQUEST = "1"
    ORDER_CANCEL_REPLACE_REQUEST = "2"


class MarketDataRejectReasonEnum(Enum):
    UNKNOWN_SYMBOL = "0"
    DUPLICATE_MD_REQ_ID = "1"
    UNSUPPORTED_SUBSCRIPTION_REQUEST_TYPE = "4"
    UNSUPPORTED_MARKET_DEPTH = "5"
    UNSUPPORTED_MD_UPDATE_TYPE = "6"
    UNSUPPORTED_MD_ENTRY_TYPE = "8"


class MarketDataSubscriptionRequestTypeEnum(Enum):
    SNAPSHOT = "0"
    SUBSCRIBE = "1"
    UNSUBSCRIBE = "2"


class MarketDataEntryTypeEnum(Enum):
    OPEN = "4"
    CLOSE = "5"
    HIGH = "7"
    LOW = "8"
