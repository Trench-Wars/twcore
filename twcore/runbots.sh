#!/bin/bash

set -e

ctrl_c() {
  exit 0
}

main() {
  java -jar bin/twcore.jar bin/setup.cfg
}

trap ctrl_c INT TERM
while true
  do main
done
