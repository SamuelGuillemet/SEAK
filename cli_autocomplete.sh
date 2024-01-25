#!/bin/bash

_cli_autocomplete() {
  local cur prev commands docker_actions java_projects python_projects java_actions python_actions

  COMPREPLY=()
  cur="${COMP_WORDS[COMP_CWORD]}"
  prev="${COMP_WORDS[COMP_CWORD - 1]}"
  commands="docker docker_multi reload_grafana reload_prometheus project help"
  docker_actions="start restart stop down"
  java_projects="market-matcher order-book order-stream trade-stream quickfix-server"
  python_projects="pre-processing"
  java_actions="build run test"
  python_actions="run test"

  case "$prev" in
  docker | docker_multi)
    mapfile -t COMPREPLY < <(compgen -W "$docker_actions" -- "$cur")
    return 0
    ;;
  project)
    mapfile -t COMPREPLY < <(compgen -W "$java_projects $python_projects" -- "$cur")
    return 0
    ;;
  # Any of the java projects
  market-matcher | order-book | order-stream | trade-stream | quickfix-server)
    mapfile -t COMPREPLY < <(compgen -W "$java_actions" -- "$cur")
    return 0
    ;;
  # Any of the python projects
  pre-processing)
    mapfile -t COMPREPLY < <(compgen -W "$python_actions" -- "$cur")
    return 0
    ;;
  *)
    mapfile -t COMPREPLY < <(compgen -W "$commands" -- "$cur")
    return 0
    ;;
  esac
}

complete -F _cli_autocomplete ./cli.sh
