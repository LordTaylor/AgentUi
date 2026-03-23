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
- [x] **Plugin Architecture**: Dynamic loading of custom tool UI and themes.
- [x] **Workspace Indexing (Local RAG)**: Native support for project-wide semantic search.
- [x] **Agentic Workflows**: Visual builder for multi-step tasks and automated loops.
- [x] **Streaming Log Viewer**: Real-time internal engine logs for developers.
- [x] **Global Scratchpad**: Shared persistent storage for snippets and session notes.
- [x] **Terminal Communication Viewer**: Real-time CLI/STDIO raw traffic inspector.

## Phase 6: Advanced Intelligence & Multimodal (COMPLETE ✅)
- [x] **Voice & Accessibility**: Integrated STT/TTS for hands-free interaction.
- [x] **Interactive Canvas**: Shared drawing/prototyping space for multimodal agents.
- [x] **Interactive Help & Examples**: Comprehensive guidance and usage examples for all features.
- [x] **Hierarchical Orchestration**: UI for managing "Leader" and "Worker" agent groups.
- [x] **Predictive Context Injection**: Smart UI that suggests files based on the current conversation.

## Phase 7: UI Polish & UX Excellence (COMPLETE ✅ 🎨)
- [x] **Animations & Transitions**: Fluid side-panel movements and shared element transitions.
- [x] **Premium Aesthetics**: Glassmorphism, Midnight Dark Theme, and refined typography.
- [x] **Layout Flexibility**: Draggable split-panes and resizable tool windows.
- [x] **Advanced Chat UI**: Message grouping, relative timestamps, and enhanced code blocks.
- [x] **Keyboard Shortcuts**: Global shortcuts (Ctrl+K new session, Ctrl+L clear, Ctrl+, settings).
- [x] **Theme Switcher**: Light / Dark / System-default toggle saved to preferences.
- [ ] **Font Size Controls**: Configurable chat font size (12–20sp) stored in local settings.
- [x] **Empty States**: Informative placeholder views when no messages / no sessions / no tools.
- [x] **Connection Status Banner**: Persistent top-bar indicator when backend is unreachable.

## Phase 8: Protocol Alignment — CoreApp v1.5 (COMPLETE ✅ 📋)
> Align Kotlin IPC layer with the full agent-core protocol v1.5 spec.

### IpcModels — Payload Fixes
- [x] **Fix `SessionsListPayload`**: Change `sessions: List<String>` → `sessions: List<SessionInfo>` with full metadata.
- [x] **Fix `SessionDataPayload`**: Add missing fields `backend`, `tags`, `created_at`, `updated_at`.
- [x] **Fix `MessageCompletePayload`**: Rename event from `message_complete` → `message_end`.
- [x] **Fix `SendMessagePayload`**: Add `include_stats: Boolean = false` and `images: List<String>? = null`.
- [x] **Add `StatusPayload.state` enum validation**: Enforce `idle|thinking|executing|waiting_approval|cancelled`.

### Missing IPC Commands
- [x] **`cancel`**: Abort a running agent session.
- [x] **`ping`**: Backend health check.
- [x] **`list_backends`**: List available AI backends (UI selection added).
- [x] **`delete_session`**: Remove a session file from disk (Sidebar icon added).
- [x] **`prune_session`**: Truncate session history (Sidebar action added).
- [x] **`tag_session` / `list_sessions_by_tag`**: Session tagging UI with filter chips in sidebar.
- [x] **`get_tool`**: Fetch full schema for a single tool (Integrated in ToolExplorer).
- [ ] **`create_tool` / `update_tool` / `delete_tool`**: Tool management CRUD from the UI.
- [ ] **`enable_tool` / `disable_tool`**: Toggle individual tools on/off in PluginManager.
- [x] **`fork_session`**: Duplicate session from a chosen message index (Chat bubble action added).
- [x] **`get_config` / `update_config`**: Read and live-patch backend config from Settings UI.
- [x] **`set_system_prompt`**: Override agent system prompt per session (Settings Dialog added).
- [x] **`reload_tools`**: Force tool registry refresh (ToolExplorer button added).
- [x] **`summarize_context`**: Trigger context compression from header (TokenTracker added).
- [x] **`schedule_task`**: Scheduling intent added to ViewModel.

### Missing IPC Events
- [x] **`thought`**: Display ReAct `Thought:` blocks in chat as collapsible reasoning bubbles.
- [x] **`tool_progress`**: Stream tool output line-by-line inside a live tool-call card.
- [x] **`tool_created`**: Toast notification when agent self-creates a new tool.
- [x] **`human_input_request`**: Modal dialog asking user a question mid-run.
- [x] **`ping_result`**: Show server version and uptime in Connection status bar.
- [x] **`backends_list`**: Populate backend dropdown in Settings from live server response.
- [x] **`task_scheduled`**: Confirmation toast with `next_fire` timestamp in WorkflowBuilder.
- [x] **`config`**: Populate Settings fields from server's live config response.

### AgentClient Improvements
- [x] **Configurable server URL**: Replace hardcoded `localhost:7700` with Settings field + persistence.
- [x] **Health check on connect**: Call `GET /health` before entering chat; show error if unreachable.
- [ ] **`GET /metrics` endpoint**: Expose Prometheus metrics in a new Metrics panel.
- [x] **Protocol version handshake**: Read `protocol_version` from `message_start`; warn if mismatch.
- [x] **Retry + exponential backoff**: Auto-reconnect SSE stream on disconnect (3 attempts, 2/4/8 s).

## Phase 15: Adaptive UI & Responsiveness (COMPLETE ✅ 📱)
- [x] **Adaptive Connection Screen**: Implemented `BoxWithConstraints` for layout switching based on available width.
- [x] **Dynamic Columns**: Connection cards intelligently transition between 1, 2, and 4 columns.
- [x] **Responsive Sidebar**: Automatically hidden on small viewports to prioritize connectivity controls.
- [x] **Vertical Scrolling**: Added main content scrolling to support narrow heights.
- [x] **Adaptive Header**: Contextual information (System Status, Admin Root) adjusts visibility for mobile-first experience.

## Phase 9: Modern Architecture & Stability (DONE ✅ 🏗️)
- [x] **MVI Pattern**: Predictable unidirectional data flow with `UiState` and `Intent`.
- [x] **Dependency Injection**: Koin integration for modular and testable components.
- [x] **Functional IPC**: Callback-based event handling for decoupled networking.
- [x] **Unit Testing**: Suite for `IpcHandler` and Core logic (JVM/Desktop target).
- [x] **Code Quality**: Strategy for small, focused files (Divide and Conquer).
- [x] **Ktor Client**: Modern asynchronous networking for IPC communication.

## Phase 9: Advanced Features Integration (PLANNED 🚀)
> Surface CoreApp capabilities that are implemented server-side but have no UI yet.

- [ ] **Vision / Image Input**: Attach images to messages (📎 + image picker); send via `images: [path]` in `SendMessagePayload`.
- [ ] **ReAct Mode Toggle**: Switch in Settings to enable `--react` mode; render `Thought:` bubbles inline.
- [ ] **Session Branching UI**: Right-click message → "Fork from here" → opens new session tab.
- [ ] **Tool Detail View**: Click a tool in ToolExplorer → full schema, arguments, description, run count.
- [ ] **Inline Tool Editor**: Create / edit Python tool files from within the app (syntax-highlighted editor).
- [ ] **Scheduler Panel**: Calendar/cron view in WorkflowBuilder to schedule recurring agent tasks.
- [ ] **Backend Health Dashboard**: Live latency, error rate, circuit breaker state per backend.
- [ ] **Session Archive Browser**: Browse `~/.agentcore/sessions/archive/` in a separate Sidebar section.
- [ ] **Context Compression Indicator**: Show context usage % in header; "Compress" button at >80%.
- [ ] **Multi-Modal Output**: Render images generated by agent (`generate_image` tool) inline in chat.
- [ ] **Checkpoint Restore**: List session checkpoints; restore to any prior state from context menu.
- [ ] **Hook Management UI**: View, enable, and disable Python hook scripts in a dedicated panel.

## Phase 10: Testing & Quality (PLANNED 🧪)
- [ ] **UI Component Tests**: JUnit5 + Compose test rules for `ChatBubble`, `ApprovalDialog`, `TokenTracker`.
- [ ] **IpcHandler Unit Tests**: Mock `IpcEvent` stream → verify state transitions in `ChatMainScreen`.
- [ ] **AgentClient Integration Tests**: Spin up a mock HTTP server; test SSE parsing and reconnect logic.
- [ ] **Screenshot Tests**: Golden-image tests for key screens (ConnectionScreen, ChatMainScreen, SettingsDialog).
- [ ] **End-to-End Smoke Test**: Gradle task that launches app + mock server, sends a message, verifies response renders.
- [ ] **Lint & Detekt**: Add Detekt static analysis config; enforce in CI.
- [ ] **CI Pipeline**: GitHub Actions workflow — build, test, package for macOS / Linux / Windows.

## Phase 11: Performance & Reliability (COMPLETE ✅ ⚡)
- [x] **Message List Virtualization**: Implemented stable keys in `LazyColumn` for efficient rendering.
- [x] **Image Loading**: Integrated Coil3 for asynchronous image decoding and caching.
- [x] **ViewModel Architecture**: Refactored `ChatMainScreen` to use a robust `ChatViewModel`.
- [x] **UI State Persistence**: Automatic saving/restoring of panel visibility and layout via `SettingsManager`.
- [x] **Offline History**: Implemented `SessionCache` for browsing previous messages without backend.
- [x] **Memory & Startup**: Reduced transient state in UI layer; optimized dependency injection.

## Phase 12: Distribution & Packaging (PLANNED 📦)
- [ ] **Auto-Updater**: Check GitHub releases on startup; prompt user to download new version.
- [ ] **macOS Code Signing & Notarization**: Sign `.app` with Developer ID; notarize for Gatekeeper.
- [ ] **Windows MSIX Package**: Modern packaging for Microsoft Store submission.
- [ ] **Linux AppImage**: Universal Linux package alongside existing DEB/RPM.
- [ ] **First-Run Setup Wizard**: Detect missing backend; guide user through `cargo install agent-core`.
- [ ] **Bundled JRE**: Include custom JRE via `jlink` to eliminate "Java not found" on end-user machines.
- [ ] **Release Automation**: GitHub Actions workflow to build all platforms and publish GitHub Release.
- [ ] **Direct File-system Editing**: Allow agent to open files in user's default editor (VSCode/JetBrains) via a specific tool.
- [ ] **Voice Waveform Visualization**: Visual feedback during agent speaking / user listening.
- [ ] **Multi-Agent Heatmap**: Visual representation of token usage and contribution per agent in a debate.
- [ ] **Session Branching UI**: Visual tree explorer for session forks and checkpoints.
- [ ] **Pinned Context Panel**: Persistent panel for files that should stay in the context indefinitely.
- [ ] **Export to PDF/HTML**: High-fidelity export of conversation history with code formatting.
- [ ] **Local Model Manager**: UI for downloading and status monitoring of Ollama/LM Studio models.
- [ ] **Advanced Prompt Library**: Pre-defined, community-driven system prompts for specific tasks (Coding, Analysis, Creative).
