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

## Phase 7: UI Polish & UX Excellence (IN PROGRESS 🎨)
- [ ] **Animations & Transitions**: Fluid side-panel movements and shared element transitions.
- [ ] **Premium Aesthetics**: Glassmorphism, Midnight Dark Theme, and refined typography.
- [ ] **Layout Flexibility**: Draggable split-panes and resizable tool windows.
- [ ] **Advanced Chat UI**: Message grouping, relative timestamps, and enhanced code blocks.
- [ ] **Keyboard Shortcuts**: Global shortcuts (Ctrl+K new session, Ctrl+L clear, Ctrl+, settings).
- [ ] **Theme Switcher**: Light / Dark / System-default toggle saved to preferences.
- [ ] **Font Size Controls**: Configurable chat font size (12–20sp) stored in local settings.
- [ ] **Empty States**: Informative placeholder views when no messages / no sessions / no tools.
- [ ] **Connection Status Banner**: Persistent top-bar indicator when backend is unreachable.

## Phase 8: Protocol Alignment — CoreApp v1.5 (PLANNED 📋)
> Align Kotlin IPC layer with the full agent-core protocol v1.5 spec.

### IpcModels — Payload Fixes
- [ ] **Fix `SessionsListPayload`**: Change `sessions: List<String>` → `sessions: List<SessionInfo>` with full metadata (`id`, `backend`, `role`, `message_count`, `tags`, `created_at`, `updated_at`).
- [ ] **Fix `SessionDataPayload`**: Add missing fields `backend`, `tags`, `created_at`, `updated_at`.
- [ ] **Fix `MessageCompletePayload`**: Rename event from `message_complete` → `message_end`; payload to `usage: UsagePayload(input_tokens, output_tokens)`.
- [ ] **Fix `SendMessagePayload`**: Add `include_stats: Boolean = false` and `images: List<String>? = null` for vision support.
- [ ] **Add `StatusPayload.state` enum validation**: Enforce `idle|thinking|executing|waiting_approval|cancelled`.

### Missing IPC Commands
- [ ] **`cancel`**: Abort a running agent session (`IpcCommand.Cancel(session_id)`).
- [ ] **`ping`**: Backend health check; handle `IpcEvent.PingResult(version, uptime_secs)`.
- [ ] **`list_backends`**: List available AI backends; handle `IpcEvent.BackendsList`.
- [ ] **`delete_session`**: Remove a session file from disk.
- [ ] **`prune_session`**: Truncate session history to the last N messages.
- [ ] **`tag_session` / `list_sessions_by_tag`**: Session tagging UI with filter chips in sidebar.
- [ ] **`get_tool`**: Fetch full schema for a single tool (for detail view in ToolExplorer).
- [ ] **`create_tool` / `update_tool` / `delete_tool`**: Tool management CRUD from the UI.
- [ ] **`enable_tool` / `disable_tool`**: Toggle individual tools on/off in PluginManager.
- [ ] **`fork_session`**: Duplicate session from a chosen message index (branching experiments).
- [ ] **`get_config` / `update_config`**: Read and live-patch backend config from Settings UI.
- [ ] **`set_system_prompt`**: Override agent system prompt per session.
- [ ] **`reload_tools`**: Force tool registry refresh button in ToolExplorer.
- [ ] **`summarize_context`**: Trigger context compression from chat overflow indicator.
- [ ] **`schedule_task` / `cancel_scheduled_task` / `list_scheduled_tasks`**: Full scheduler UI in WorkflowBuilder.

### Missing IPC Events
- [ ] **`thought`**: Display ReAct `Thought:` blocks in chat as collapsible reasoning bubbles.
- [ ] **`tool_progress`**: Stream tool output line-by-line inside a live tool-call card.
- [ ] **`tool_created`**: Toast notification when agent self-creates a new tool.
- [ ] **`human_input_request`**: Modal dialog asking user a question mid-run.
- [ ] **`ping_result`**: Show server version and uptime in Connection status bar.
- [ ] **`backends_list`**: Populate backend dropdown in Settings from live server response.
- [ ] **`task_scheduled`**: Confirmation toast with `next_fire` timestamp in WorkflowBuilder.
- [ ] **`config`**: Populate Settings fields from server's live config response.

### AgentClient Improvements
- [ ] **Configurable server URL**: Replace hardcoded `localhost:7700` with Settings field + persistence.
- [ ] **Health check on connect**: Call `GET /health` before entering chat; show error if unreachable.
- [ ] **`GET /metrics` endpoint**: Expose Prometheus metrics in a new Metrics panel.
- [ ] **Protocol version handshake**: Read `protocol_version` from `message_start`; warn if mismatch.
- [ ] **Retry + exponential backoff**: Auto-reconnect SSE stream on disconnect (3 attempts, 2/4/8 s).

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

## Phase 11: Performance & Reliability (PLANNED ⚡)
- [ ] **Message List Virtualization**: Ensure `LazyColumn` uses `key {}` for stable recomposition on large histories.
- [ ] **Image Lazy Loading**: Decode and cache images off the main thread (Coil or custom).
- [ ] **ViewModel Extraction**: Move `ChatMainScreen` state into a `ViewModel` (lifecycle-safe, testable).
- [ ] **State Persistence**: Save/restore UI state across restarts (last session, panel visibility, input draft).
- [ ] **Memory Profiling**: Identify and fix leaks in long-running sessions (coroutine scope leaks, bitmap cache).
- [ ] **Startup Time**: Profile and optimize cold-start; lazy-init non-critical components.
- [ ] **Offline Mode**: Graceful degradation when backend is not running — show last session read-only.

## Phase 12: Distribution & Packaging (PLANNED 📦)
- [ ] **Auto-Updater**: Check GitHub releases on startup; prompt user to download new version.
- [ ] **macOS Code Signing & Notarization**: Sign `.app` with Developer ID; notarize for Gatekeeper.
- [ ] **Windows MSIX Package**: Modern packaging for Microsoft Store submission.
- [ ] **Linux AppImage**: Universal Linux package alongside existing DEB/RPM.
- [ ] **First-Run Setup Wizard**: Detect missing backend; guide user through `cargo install agent-core`.
- [ ] **Bundled JRE**: Include custom JRE via `jlink` to eliminate "Java not found" on end-user machines.
- [ ] **Release Automation**: GitHub Actions workflow to build all platforms and publish GitHub Release.
