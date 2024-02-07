#!/bin/bash

source cli_autocomplete.sh

CONFIG_FILE="./config/common/kafka.yml"
JAVA_PROJECTS=("market-matcher" "order-book" "order-stream" "trade-stream" "quickfix-server")
PYTHON_PROJECTS=("pre-processing" "website-backend")
NPM_PROJECTS=("website-frontend")

function docker_single() {
  base_command=("docker" "compose" "-f" "docker-compose.base.yml" "-f" "docker-compose.single.yml")

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
    echo "Valid actions: start, restart, stop, down"
    exit 1
  fi

  BOOTSTRAP_SERVERS="localhost:9092"

  sed -i 's/^\(\s*bootstrap.servers\s*:\s*\).*/\1'"$BOOTSTRAP_SERVERS"'/' "$CONFIG_FILE"
}

function docker_multi() {
  base_command=("docker" "compose" "-f" "docker-compose.base.yml" "-f" "docker-compose.multi.yml")

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
    echo "Valid actions: start, restart, stop, down"
    exit 1
  fi

  BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"

  sed -i 's/^\(\s*bootstrap.servers\s*:\s*\).*/\1'"$BOOTSTRAP_SERVERS"'/' "$CONFIG_FILE"
}

function project() {
  project=$1
  action=$2
  # If action is not specified, then default to run
  if [ -z "$action" ]; then
    action="run"
  fi
  args=("$@")

  # Test if project is java
  if [[ " ${JAVA_PROJECTS[*]} " =~ ${project} ]]; then
    if [ "$action" == "test" ]; then
      ./gradlew :components:"$project":test "${args[@]:2}"
    elif [ "$action" == "run" ]; then
      ./gradlew :components:"$project":run
    elif [ "$action" == "build" ]; then
      ./gradlew :components:"$project":build
    elif [ "$action" == "coverage" ]; then
      ./gradlew :components:"$project":jacocoTestReport
    else
      echo "Invalid action for project: $action"
      echo "Valid actions: build, run, test coverage"
      exit 1
    fi
  # Test if project is python
  elif [[ " ${PYTHON_PROJECTS[*]} " =~ ${project} ]]; then
    if [ "$action" == "run" ]; then
      if [ "$project" == "website-backend" ]; then
        (
          cd components/"$project" || exit
          DB_TYPE=POSTGRES poetry run uvicorn app.main:app --port 8001
        )
      else
        (
          cd components/"$project" || exit
          poetry run "$project" "${args[@]:2}"
        )
      fi
    elif [ "$action" == "test" ]; then
      (
        cd components/"$project" || exit
        poetry run pytest tests/ "${args[@]:2}"
      )
    else
      echo "Invalid action for project: $action"
      echo "Valid actions: run, test"
      exit 1
    fi
  # Test if project is npm
  elif [[ " ${NPM_PROJECTS[*]} " =~ ${project} ]]; then
    if [ "$action" == "run" ]; then
      (
        cd components/"$project" || exit
        npm run dev
      )
    elif [ "$action" == "build" ]; then
      (
        cd components/"$project" || exit
        npm run build
      )
    elif [ "$action" == "start" ]; then
      (
        cd components/"$project" || exit
        npm start
      )
    elif [ "$action" == "test" ]; then
      (
        cd components/"$project" || exit
        npm run test
      )
    else
      echo "Invalid action for project: $action"
      echo "Valid actions: run build start test"
      exit 1
    fi
  else
    echo "Invalid project: $project"
    echo "Valid projects: ${JAVA_PROJECTS[*]} ${PYTHON_PROJECTS[*]}"
    exit 1
  fi
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
  docker <action>                       Manage docker containers with kafka having single node
  docker_multi <action>                 Manage docker containers with kafka having multiple nodes
  reload_grafana                        Reload grafana dashboards
  reload_prometheus                     Reload prometheus config
  help                                  Show this help text
  project <project> <action> [options]  Manage projects

  Docker actions:
    start                Start containers
    restart              Restart containers
    stop                 Stop containers
    down                 Stop and remove containers

  Project actions:
    build                Build project
    run                  Run project
    test                 Run tests
    coverage             Run tests and generate coverage report"

if [ "$#" -lt 1 ]; then
  echo "$help_text"
  exit 1
fi

command="$1"
shift

case "$command" in
"docker")
  docker_single "$@"
  ;;
"docker_multi")
  docker_multi "$@"
  ;;
"reload_grafana")
  reload_grafana
  ;;
"reload_prometheus")
  reload_prometheus
  ;;
"project")
  project "$@"
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
