#!/bin/bash

CONFIG_FILE="./config/common/kafka.yml"

function manage_single_node() {
  base_command=("docker-compose" "-f" "docker-compose.base.yml" "-f" "docker-compose.single.yml")

  if [ "$1" == "start" ]; then
    "${base_command[@]}" up -d
  elif [ "$1" == "restart" ]; then
    "${base_command[@]}" restart
  elif [ "$1" == "stop" ]; then
    "${base_command[@]}" stop
  elif [ "$1" == "down" ]; then
    "${base_command[@]}" down
  else
    echo "Invalid action for docker: $1"
    exit 1
  fi

  BOOTSTRAP_SERVERS="localhost:9092"

  sed -i 's/^\(\s*bootstrap.servers\s*:\s*\).*/\1'"$BOOTSTRAP_SERVERS"'/' "$CONFIG_FILE"
}

function manage_multiple_nodes() {
  base_command=("docker-compose" "-f" "docker-compose.base.yml" "-f" "docker-compose.multi.yml")

  if [ "$1" == "start" ]; then
    "${base_command[@]}" up -d
  elif [ "$1" == "restart" ]; then
    "${base_command[@]}" restart
  elif [ "$1" == "stop" ]; then
    "${base_command[@]}" stop
  elif [ "$1" == "down" ]; then
    "${base_command[@]}" down
  else
    echo "Invalid action for docker in kafka multi node: $1"
    exit 1
  fi

  BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"

  sed -i 's/^\(\s*bootstrap.servers\s*:\s*\).*/\1'"$BOOTSTRAP_SERVERS"'/' "$CONFIG_FILE"
}

function reload_grafana() {
  curl -X POST "http://admin:admin@localhost:3000/api/admin/provisioning/dashboards/reload"
  echo ""
}

function reload_prometheus() {
  curl -X POST "http://localhost:9090/-/reload"
  echo "Prometheus config reloaded"
}

help_text="Usage: $0 <command> [options]

Commands:
  docker <action>        Manage docker containers with kafka having single node
  docker_multi <action>  Manage docker containers with kafka having multiple nodes
  reload_grafana         Reload grafana dashboards
  reload_prometheus      Reload prometheus config
  help                   Show this help text

  Actions:
    start                Start containers
    restart              Restart containers
    stop                 Stop containers
    down                 Stop and remove containers"

if [ "$#" -lt 1 ]; then
  echo "$help_text"
  exit 1
fi

command="$1"
shift

case "$command" in
"docker")
  manage_single_node "$@"
  ;;
"docker_multi")
  manage_multiple_nodes "$@"
  ;;
"reload_grafana")
  reload_grafana
  ;;
"reload_prometheus")
  reload_prometheus
  ;;
"help" | "-h" | "--help")
  echo "$help_text"
  ;;
*)
  echo "Invalid command: $command"
  echo "$help_text"
  exit 1
  ;;
esac
