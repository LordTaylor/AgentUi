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
- **`TerminalViewer`**: Raw IPC traffic inspector for real-time debugging.
- **`IndexingStatus`**: Progress indicator for workspace semantic indexing (Local RAG).
- **`PluginManager`**: Dynamic extension and tool management interface.
- **`WorkflowBuilder`**: Visual orchestrator for multi-step agentic tasks.
- **`InteractiveCanvas`**: Shared drawing and prototyping space for multimodal agents.
- **`HelpSystem`**: Interactive guidance system with usage examples for all features.
- **`AgentOrchestrator`**: Hierarchical visualization and task delegation for multi-agent teams.
- **`PredictiveContext`**: Smart UI component for context-aware file and resource suggestions.

## Core Principles

- **Divide and Conquer**: Avoid large monolithic files. Always split code into smaller, focused modules and components.
- **IPC-First**: Communication between the UI and the Backend must happen via the strictly typed IPC protocol.

### 3. Protocol (`core-api`)
Unified IPC models shared between client and core.
- **Current Version**: v1.5 (target: v1.6)
- **Features**: Approval requests, streaming events, session persistence, scheduling, multi-modal images, tool management, backend plugins, context summarisation.

## Security & Safety
Interactive Tool Approval ensures that critical tools (file deletion, shell execution) require explicit user consent before execution.
