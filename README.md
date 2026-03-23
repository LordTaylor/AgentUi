# AgentCore UI

Desktop application for `agent-core` built with Kotlin Multiplatform and Compose.

## Quick Start

### 1. Requirements
- JDK 17 or higher
- `agent-core` binary installed in `~/.local/bin/agent-core`

### 2. Run from Source
```bash
./gradlew :composeApp:run
```

### 3. Build Desktop Package
```bash
./gradlew :composeApp:package
```
The resulting installers will be in `composeApp/build/compose/binaries/main`.

## Configuration

The UI connects to the Rust backend via `stdio` by default. You can configure the backend (Claude, OpenAI, Google, LM Studio) and its remote URL directly in the **Settings** panel within the application.

## Troubleshooting

### Backend not responding
1. Check if `agent-core` is building successfully in the `CoreApp` directory.
2. Check logs in `~/.agentcore/logs/` for specific error messages from the LLM provider.
3. If using **LM Studio**, ensure the **Context Length** is large enough (recommended: 16k+) to accommodate the system prompt and tool definitions.

For more detailed documentation, see [docs/README.md](docs/README.md).
