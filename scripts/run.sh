#!/bin/bash
# Script to run the Agent Core Desktop UI

echo "Checking agent-core status..."
# The UI will auto-launch the core, but we can also ensure environment is set.

echo "Starting Agent Core UI via Gradle..."
cd "$(dirname "$0")/.."
./gradlew :composeApp:run
