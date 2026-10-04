#!/bin/bash

# Starts battery in the foreground with an interactive shell.
#
# Usage:
#   ./run.sh                      pick a profile from a menu
#   ./run.sh --profiles bank      start with the 'bank' profile
#   ./run.sh --help               print all command line options

profiles=""
app_jarfile=battery.jar

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

menu() {
  echo ""
  echo "All 'application-*.yml' profiles:"
  echo ""

  options=($(find config/ -name '*.yml' | awk -F'/' '{print $NF}' | sed -E 's/^application-//; s/\.[^.]*$//' ))

  if [ ${#options[@]} -eq 0 ]; then
    echo "No 'config/**/application-*.yml' files found"
    exit 1
  fi

  PS3='Select profile: '

  select option in "${options[@]}";  do
    case $option in
      *)
        profiles=$option
        break
        ;;
    esac
  done

  echo ""
  echo "You picked the '$profiles' profile(s)."
  echo ""

  java -jar "$app_jarfile" --profiles "$profiles"
}

if [ $# -eq 0 ]; then
    menu
else
    java -jar "$app_jarfile" "$@"
fi
