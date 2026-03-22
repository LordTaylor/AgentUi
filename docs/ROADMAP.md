# Project Roadmap

## Phase 1: Foundation (DONE ✅)
- [x] Multi-module Gradle Structure (`:composeApp`, `:shared`, `:core-api`)
- [x] Basic Chat UI with Material3 and Dark Theme
- [x] Connection Strategy Selection (IPC vs CLI vs STDIO)
- [x] Automatic Core Launcher
- [x] Stdio Persistence for real-time streaming
- [x] Handled non-streaming HTTP IPC (v1.2)

## Phase 2: Core Features (DONE ✅)
- [x] **Session Management**: Sidebar to list, load, and delete past sessions.
- [x] **Settings Dashboard**: Runtime backend and role switching.
- [x] **File Attachments**: Support for context injection via 📎 icon.
- [x] **Markdown Rendering**: Rich text formatting for agent responses.

## Phase 3: Advanced Agentic UI (DONE ✅)
- [x] **Stats Dashboard**: Real-time metrics tracking (tokens, cost, iterations).
- [x] **Tool Explorer**: Browsing and inspecting available agent capabilities.
- [x] **Modular Architecture**: Split UI into granular, token-efficient components.
- [x] **Interactive Tool Approval**: UI prompt for tools requiring confirmation.

## Phase 4: Distribution & Advanced Transport (DONE ✅)
- [x] **Multi-Platform Installers**: Windows (MSI, EXE), Linux (DEB, RPM), macOS (DMG).
- [x] **Unix Socket Transport**: Native high-performance streaming (Java 17).
- [x] **Token Usage Tracker**: Real-time cost indicator in the header.
- [x] **Custom Syntax Highlighting**: Focused highlighting for code blocks.
- [x] **Multi-Agent Collaboration**: UI for agent consultations and debates.
- [x] **Autonomous Vision**: Enhanced image and screenshot viewing.

## Phase 5: Ecosystem & Orchestration (PLANNED 🚀)
- [ ] **Plugin Architecture**: Dynamic loading of custom tool UI and themes.
- [ ] **Workspace Indexing (Local RAG)**: Native support for project-wide semantic search.
- [ ] **Agentic Workflows**: Visual builder for multi-step tasks and automated loops.
- [x] **Streaming Log Viewer**: Real-time internal engine logs for developers.
- [x] **Global Scratchpad**: Shared persistent storage for snippets and session notes.
- [ ] **Terminal Communication Viewer**: Real-time CLI/STDIO raw traffic inspector.

## Phase 6: Advanced Intelligence & Multimodal (FUTURE 🔮)
- [ ] **Voice & Accessibility**: Integrated STT/TTS for hands-free interaction.
- [ ] **Interactive Canvas**: Shared drawing/prototyping space for multimodal agents.
- [ ] **Hierarchical Orchestration**: UI for managing "Leader" and "Worker" agent groups.
- [ ] **Predictive Context Injection**: Smart UI that suggests files based on the current conversation.
