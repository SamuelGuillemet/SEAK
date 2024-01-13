# Quickfix client

This repository comprises a Python-based client application designed to interact with a broker's system using the QuickFIX library. The architecture is structured as follows:

## Global Architecture Overview

### Entry Point
- **main.py**: This file serves as the entry point of the application. It initializes and starts the main functionality.

### Functionality Modules
- **application.py**: Contains the core logic of the quickfix application.
- **constant.py**: Stores constants used throughout the application.
- **decorators.py**: Defines decorators used within the application.

### Handlers
- **handlers/**: This directory holds modules responsible for handling specific message types. `execution_report.py` focuses on handling execution reports from the broker's system.

### Utility Modules
- **utils/**: Houses utility modules utilized across the application:
  - `loader.py`: Handles loading operations.
  - `logger.py`: Provides logging functionality.
  - `quickfix.py`: Offers utility functions for interfacing with the QuickFIX library.

### Wrappers
- **wrappers/**: Encapsulates and extends functionality from the QuickFIX library:
  - `enums.py`: Houses enumerations and constants related to QuickFIX.
  - `execution_report.py`: Wrapper dedicated to store execution reports as python objects.
  - `new_order_single.py`: Wrapper facilitating the handling of new single orders, for example its creation and storage as a python object.

The architecture follows a modular approach, segregating functionalities into discrete modules and directories. It aims to streamline interactions with the QuickFIX library and broker system by providing wrappers and utilities while maintaining clear separation of concerns.


## Specifications

In order to interact flawlessly with the quickfix application, you can define callback functions when instantiating the application. These callbacks are defined as follows:

```python
execution_handler = ExecutionReportHandler(
    on_filled_report=lambda report: logger.info(f"Filled: {report}"),
    on_rejected_report=lambda report: logger.info(f"Rejected: {report}"),
)
application, initiator = setup()
application.set_execution_report_handler(execution_handler)
```

Callbacks function should take as input a python object representing the execution report. For example we have:
  
```python
@dataclass
class FilledExecutionReport:
    order_id: int
    client_order_id: int
    symbol: str
    side: SideEnum
    type: OrderTypeEnum
    leaves_quantity: int
    price: float
    cum_quantity: int


@dataclass
class RejectedExecutionReport:
    order_id: int
    client_order_id: int
    symbol: str
    side: SideEnum
    type: OrderTypeEnum
    leaves_quantity: int
    reject_reason: OrderRejectReasonEnum
```

In this example, we define two callbacks, one for filled reports and one for rejected reports. These callbacks are called whenever the application receives a filled or rejected report from the broker's system.

To send new orders, you can use the following function:

```python
order = NewOrderSingle.new_market_order(0, SideEnum.SELL, 1, "ACGL")
application.send(order)
```

If you want to store the order as a python object, you can use the following function:

```python
python_order = order.get_order()
```

This function returns a `Order` dataclass object, which contains the order as a python object. This object is defined as follows:

```python
@dataclass
class Order:
    order_id: int | None
    client_order_id: int
    symbol: str
    side: SideEnum
    price: float | None
    quantity: int
```

---

## Installation

To install the client, you need to run the following commands:

```bash
$ poetry install
```

## Running the client

To run the client, you need to run the following command:

```bash
(.venv) $ python broker_quickfix_client/main.py
```
