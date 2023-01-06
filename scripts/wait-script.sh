#!/usr/bin/env bash

echo 'Start waiting'

IFS="," read -ra PORTS <<<"$WAIT_PORTS"
path=$(dirname "$0")

echo 'Main proc ==>'

PIDs=()
for port in "${PORTS[@]}"; do
  echo $port
  "$path"/wait-for.sh -t 120 "http://localhost:$port/manage/health" -- echo "Host localhost:$port is active" &
  PIDs+=($!)
done

for pid in "${PIDs[@]}"; do
  if ! wait "${pid}"; then
    exit 1
  fi
done
