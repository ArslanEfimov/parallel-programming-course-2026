#!/bin/bash

set -e

STAGE=$1

if [ -z "$STAGE" ]; then
    echo "Usage: ./run-benchmark.sh <stage>"
    echo "Example: ./run-benchmark.sh 0"

    exit 1

fi
rm -rf out
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out "org.lab1.stages.stage${STAGE}.Stage${STAGE}Main"