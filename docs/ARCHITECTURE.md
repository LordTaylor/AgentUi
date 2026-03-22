# AgentCore Architecture

## Overview
AgentCore is a cross-platform desktop interface for the `agent-core` Rust engine, built with Kotlin Multiplatform and Compose.

## Key Components

### 1. Transport Layer (`shared`)
Decouples backend communication from the UI.
- **`UnixSocketExecutor`**: Native Java 17 `SocketChannel` for `~/.agentcore/agent.sock`.
- **`StdioExecutor`**: Process-based streaming via standard I/O.
- **`AgentClient`**: HTTP-based IPC client for long-running servers.

### 2. UI Registry (`composeApp`)
Granular components for performance and maintainability.
- **`ChatBubble`**: Rich Markdown rendering with custom **Syntax Highlighting**.
- **`ApprovalDialog`**: Safe interactive tool execution flow.
- **`ToolExplorer`**: Real-time agent capability inspection.
- **`StatsDashboard`**: Dynamic token and cost tracking.
- **`LogViewer`**: Real-time engine log streaming with level filtering.
- **`Scratchpad`**: Persistent markdown notes and code snippet storage.

### 3. Protocol (`core-api`)
Unified IPC models shared between client and core.
- **Current Version**: v1.3
- **Features**: Approval requests, streaming events, and session persistence.

## Security & Safety
Interactive Tool Approval ensures that critical tools (file deletion, shell execution) require explicit user consent before execution.
