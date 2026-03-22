#!/bin/bash
# Script to build the Agent Core Desktop UI distributions

RUN_AFTER_BUILD=false

for arg in "$@"; do
    if [ "$arg" == "--run" ]; then
        RUN_AFTER_BUILD=true
    fi
done

echo "Cleaning and building Agent Core UI..."
cd "$(dirname "$0")/.."
./gradlew clean :composeApp:package

if [ $? -eq 0 ]; then
    echo "Build complete. Check composeApp/build/compose/binaries/main/ for outputs."
    if [ "$RUN_AFTER_BUILD" = true ]; then
        echo "Running application..."
        ./gradlew :composeApp:run
    fi
else
    echo "Build failed."
    exit 1
fi
