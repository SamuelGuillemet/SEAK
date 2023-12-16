# Pre processing of data

## Data

The data used in this project is a large set of CSVs stored in a zip file.
Each file corresponds to a ticker and contains the following columns:

- Date
- X (Useless column)
- Open
- High
- Low
- Close
- Volume

The data is stored in the `data` folder.

## Pre processing

The goal of the pre processing is to create a single CSV file containing all the data for all the tickers but restricted to a single day.

After that we want to unify the data by adding the following columns:

- Ticker

Then each data is interpolated to have the same number of rows, by adding rows at different time intervals.

## Kafka

The data for each ticker is sent to a Kafka topic. The topic name is the ticker name.
We can specify the number of processes that will send the data to Kafka.
We can also specify the number of processes that will complete the pre processing.

## Running the pre processing

To run the pre processing, you need to run the following command:

```bash
(.venv) $ python pre_processing/main.py
```

You can use the following arguments to skip some steps:

- `--skip-topic-creation`: Skip the topic creation if the topics already exists and you don't want to delete them
- `--skip-schema-creation`: Skip the schema creation if the schema already exists and you don't want to delete it
