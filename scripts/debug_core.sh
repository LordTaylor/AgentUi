#!/bin/bash

# Navigate to CoreApp directory
cd "/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp"

echo "Starting Agent Core in DEBUG mode..."

# Run agent-core with --debug flag
cargo run -- --debug "$@"
