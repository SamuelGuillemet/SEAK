#! /bin/bash

# This script is used to stress test the engine by starting n clients

# Get the number of clients to start
if [ $# -eq 0 ]; then
  echo "No arguments supplied"
  echo "Usage: ./latency.sh <number of clients>"
  exit 1
fi

num_clients=$1

#  Read username password from users.txt file
# format is username password

for ((i = 0; i < num_clients; i++)); do
  # Start the client
  username=$(sed -n "$((i + 1))p" users.txt | awk '{print $1}')
  password=$(sed -n "$((i + 1))p" users.txt | awk '{print $2}')
  echo "Starting client $username"
  poetry run python broker_quickfix_client/main.py --username $username --password $password &
done

# Wait for the clients to finish
wait
