#! /bin/bash

# This script is used to stress test the engine by starting n clients

# Get the number of clients to start
if [ $# -eq 0 ]; then
  echo "No arguments supplied"
  echo "Usage: ./latency.sh <number of clients>"
  exit 1
fi

num_clients=$1

for ((i = 0; i < num_clients; i++)); do
  # Start the client
  poetry run python broker_quickfix_client/main.py --username user$i &
done

# Wait for the clients to finish
wait
