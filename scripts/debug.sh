#!/bin/bash

# Navigate to the project root
cd "$(dirname "$0")/.."

echo "Starting Agent Core UI in DEBUG mode..."

# Set JVM properties for debugging and enhanced logging
# Port 5005 is standard for remote debugging
./gradlew :composeApp:run \
  -Dorg.gradle.project.compose.desktop.io.check=true \
  -Dapple.awt.UIElement=false \
  -Pkotlin.compiler.execution.strategy="daemon" \
  --info \
  --stacktrace
