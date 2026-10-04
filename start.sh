#!/bin/bash

# Starts battery in the background (headless, non-interactive shell).
#
# Usage:
#   ./start.sh                              pick a profile from a menu
#   ./start.sh --profiles bank              start with the 'bank' profile
#   ./start.sh --profiles bank @cmd.txt     ..and run the shell commands in cmd.txt

profiles=""
app_jarfile=battery.jar
app_logfile=.log/battery-stdout.log

if [ ! -f "$app_jarfile" ]; then
    app_jarfile=target/battery.jar
fi

if [ ! -f "$app_jarfile" ]; then
    echo "No $app_jarfile found - run './mvnw clean install' first"
    exit 1
fi

# Matches the running server, if any. The bracket prevents grep from matching itself.
find_pid() {
  ps -ef | grep "[j]ava" | grep "battery.jar" | awk '{print $2}' | head -1
}

pid=$(find_pid)
if [ -n "$pid" ]; then
   echo "Existing process found ($pid) - is it already running?"
   exit 1
fi

start() {
  mkdir -p .log

  # Only pass --profiles when picked from the menu, otherwise it's expected
  # to be part of the caller provided arguments.
  args=()
  if [ -n "$profiles" ]; then
    args+=(--profiles "$profiles")
  fi
  args+=("$@")

  nohup java -jar "$app_jarfile" --noshell "${args[@]}" > "$app_logfile" 2>&1 &

  sleep 2

  pid=$(find_pid)

  if [ -z "$pid" ]; then
     echo "No battery.jar process found - check $app_logfile"
     exit 1
  else
     echo "Start successful (pid: $pid) - check $app_logfile"
     exit 0
  fi
}

menu() {
  echo ""
  echo "All 'application-*.yml' profiles:"
  echo ""

  options=($(find config/ -name '*.yml' | awk -F'/' '{print $NF}' | sed -E 's/^application-//; s/\.[^.]*$//'))

  if [ ${#options[@]} -eq 0 ]; then
    echo "No 'config/**/application-*.yml' files found"
    exit 1
  fi

  PS3='Select profile: '

  select option in "${options[@]}";  do
    case $option in
      *)
        profiles=$option
        echo ""
        echo "You selected: $option"
        echo ""
        break
        ;;
    esac
  done

  start
}

if [ $# -eq 0 ]; then
    menu
else
    start "$@"
fi
