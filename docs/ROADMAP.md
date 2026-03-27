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
- [x] **Auto-Save & New Session**: Persistent local cache and "New Session" quick action in top-left.
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
- [x] **Font Size Controls**: Configurable chat font size (10–24sp) slider in Settings; stored via `UiSettings.chatFontSize`/`codeFontSize`; applied dynamically in ChatBubble.
- [x] **Empty States**: Informative placeholder views when no messages / no sessions / no tools.
- [x] **Connection Status Banner**: Persistent top-bar indicator when backend is unreachable.
- [x] **Cauldron Liquid Animation Enhancements**: Cartoon wave deformation (pow), bubble hesitation effect, squash & stretch on bounce, ripple micro-waves, splash particles, dynamic bubble density (±25% fluctuation).
- [x] **Cauldron Spoon & Tech-Object Animations**: Spoon redesigned as pendulum (submerged bowl + visible handle via two-pass Y-clip rendering); RECEIVING state shows tech objects (mouse, floppy, phone, keyboard) falling into cauldron; SENDING state ejects tech objects in parabolic arcs.

## Phase 8: Protocol Alignment — CoreApp v1.6 (COMPLETE ✅ 📋)
> Align Kotlin IPC layer with the full agent-core protocol v1.6 spec.

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
- [x] **`create_tool` / `delete_tool`**: Tool creation via CreateTool dialog and deletion via SkillLibrary context menu (IPC wired). `update_tool` IPC not in schema v1.6 — deferred.
- [x] **`enable_tool` / `disable_tool`**: Toggle individual tools on/off in PluginManager; IPC commands wired through `ToggleTool` intent with optimistic UI update.
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
- [x] **`GET /metrics` endpoint**: `MetricsPanel.kt` fetches `GET /metrics` from backend; parses Prometheus text format; renders name/value rows with color-coded badges; "Metrics" tab in NarrowSidebar.
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
- [x] **Code Quality**: Strategy for small, focused files (Divide and Conquer); 240-line file limit enforced; 13 oversized files refactored into 30+ focused modules.
- [x] **Ktor Client**: Modern asynchronous networking for IPC communication.
- [x] **AgentClient Modularization**: Split into `AgentClientSessions`, `AgentClientTools`, `AgentClientStreaming` extension-function files.
- [x] **IpcModels Split**: Separated into `IpcCommand`, `IpcCommandPayloads`, `IpcEvent`, `IpcEventPayloads`, `IpcEventPayloads2`, `IpcModels` (shared types).
- [x] **Sub-Agent Thread UI (I35)**: Collapsible sub-agent thread rendering in chat (`SubAgentThread.kt`); `MessageType.SUB_AGENT_THREAD` added; thread state stored as JSON in `Message.extraContent`.
- [x] **Slash Command Parser**: `SlashCommandParser.kt` for autocomplete of `/` commands in chat input.
- [x] **Pixel Avatar Components**: `PixelGoblinAvatar`, `PixelMagicBook`, `AvatarPixelHelper` in dedicated `avatar/` sub-package.

## Phase 9: Advanced Features Integration (PLANNED 🚀)
> Surface CoreApp capabilities that are implemented server-side but have no UI yet.

- [x] **Vision / Image Input**: Attach images to messages (📎 + image picker); send via `images: [path]` in `SendMessagePayload`.
- [x] **ReAct Mode Toggle**: Switch in Settings to enable `--react` mode (UiSettings.reactMode → update_config IPC); `Thought:` bubbles already rendered inline via ThinkingBubble.
- [x] **Session Branching UI**: Right-click context menu on chat bubbles via `ContextMenuArea`; "Fork session from here", "Copy text", "Edit", "Retry" items; routes to existing `onFork` callback.
- [x] **Tool Detail View**: Click a tool in ToolExplorer → full schema, arguments, description; fetched via `get_tool` IPC; `ToolDetailDialog.kt` with parameters table + raw JSON toggle.
- [x] **Inline Tool Editor**: `ToolEditorPanel.kt` — lists `.py` files from `~/.agentcore/tools/`; `BasicTextField` editor with monospace font; Save to disk; New tool dialog with template; "ToolEditor" tab in NarrowSidebar.
- [x] **Scheduler Panel**: `SchedulerPanel.kt` — form with task text + cron/datetime toggle; list of upcoming tasks from `ScheduledTaskInfo`; wired to `ScheduleTask` ChatIntent; "Scheduler" tab in NarrowSidebar.
- [x] **Backend Health Dashboard**: `BackendHealthPanel.kt` — live latency per backend, uptime, version badge, color-coded latency (green/amber/red); `LoadBackendHealth` intent pings all backends via `pingBackend` IPC; "Health" tab in NarrowSidebar toggles panel.
- [x] **Session Archive Browser**: `SessionArchiveBrowser.kt` — lists session JSON files from `~/.agentcore/sessions/archive/`; sorted by date, shows session ID + size + timestamp; "Archive" tab in NarrowSidebar.
- [x] **Context Compression Indicator**: Show context usage % in header with animated LinearProgressIndicator (orange >60%, red >80%); "Compress" button highlights at >80% threshold.
- [x] **Multi-Modal Output**: Agent-generated images extracted from text via regex path scan + rendered inline via `InlineAgentImage` (Skia); user-attached images from `msg.attachments` list also rendered in bubble.
- [x] **Checkpoint Restore**: `CheckpointRestoreDialog.kt`; `LoadCheckpoints`/`RestoreCheckpoint` intents; SessionViewModel pings `list_checkpoints` IPC; restore button with confirmation dialog; Sidebar context menu entry.
- [x] **Hook Management UI**: `HookManagerPanel.kt` — lists `.py` hook scripts from `~/.agentcore/hooks/`; toggle enable/disable by renaming with `.disabled` suffix; inline source viewer; "Hooks" tab in NarrowSidebar.

## Phase 10: Testing & Quality (COMPLETE ✅ 🧪)
- [x] **UI Component Tests**: `IpcHandlerExtendedTest.kt` covers status transitions, text delta accumulation, tool_call, approval_request, error, log, and human_input routing.
- [x] **IpcHandler Unit Tests**: `IpcHandlerExtendedTest.kt` — 12 tests verifying state transitions for all major event types using mock callbacks.
- [x] **AgentClient Integration Tests**: `AgentClientMockTest.kt` — Ktor MockEngine; tests ping, sendCommand error handling, listTools malformed response, serialization roundtrips for all command types.
- [x] **Screenshot Tests**: `ScreenshotSmokeTest.kt` — golden-state verification for `ChatUiState` defaults, `Message` type correctness, `buildMarkdownExport`/`buildHtmlExport` output structure (HTML5 doctype, bubble classes, ACTION filtering), `SendMessagePayload` defaults, and `StatusPayload` verbatim storage.
- [x] **End-to-End Smoke Test**: `E2ESmokeTest.kt` — `AgentClient` + `MockEngine`; verifies: POST /command for send_message, SSE stream parsing (message_start → text_delta → message_end), session ID capture, text accumulation, tool_call → ACTION message, status uppercasing, listSessions response; IpcHandler dispatch chain tested end-to-end.
- [x] **Lint & Detekt**: `detekt.yml` config with 240+ rules; Compose-aware exceptions (LongParameterList, FunctionNaming, MagicNumber); registered in root `build.gradle.kts` for all subprojects.
- [x] **CI Pipeline**: `.github/workflows/build.yml` — build+test (Linux), Detekt, package DMG (macOS), DEB/RPM (Linux), MSI (Windows); GitHub Release job on tag push with artifact upload.

## Phase 11: Performance & Reliability (COMPLETE ✅ ⚡)
- [x] **Message List Virtualization**: Implemented stable keys in `LazyColumn` for efficient rendering.
- [x] **Image Loading**: Integrated Coil3 for asynchronous image decoding and caching.
- [x] **ViewModel Architecture**: Refactored `ChatMainScreen` to use a robust `ChatViewModel`.
- [x] **UI State Persistence**: Automatic saving/restoring of panel visibility and layout via `SettingsManager`.
- [x] **Offline History**: Implemented `SessionCache` for browsing previous messages without backend.
- [x] **Memory & Startup**: Reduced transient state in UI layer; optimized dependency injection.

## Phase 12: Distribution & Packaging (COMPLETE ✅ 📦)
- [x] **Auto-Updater**: `AutoUpdater.kt` — checks GitHub Releases API (`agentcore-dev/agentcore-ui`) on startup; compares semver tags; `UpdateAvailableDialog` with release notes + download link; `AutoUpdateChecker` composable with `LaunchedEffect`; `openUrlInBrowser()` via `Desktop.browse(URI(...))`.
- [x] **macOS Code Signing & Notarization**: `build.gradle.kts` — `AGENTCORE_SIGN_ID` env-var gated signing block; `notarization { appleID / password / teamID }` wired from env vars; activated automatically in CI when secrets are set.
- [x] **Windows MSIX Package**: `build.yml` `package-msix` job — creates distributable via `createDistributable`, assembles `msix_staging/` with auto-generated `AppxManifest.xml` + placeholder icons, runs `makeappx pack` (Windows SDK); `continue-on-error: true` for unsigned sideloading builds; artifact uploaded as `windows-msix`.
- [x] **Linux AppImage**: `build.yml` `package-appimage` job — installs `appimagetool`, creates `AppDir/` from `createDistributable`, writes `AppRun` + `.desktop` + SVG icon, runs `ARCH=x86_64 appimagetool`; `continue-on-error: true` for CI FUSE limitation; artifact uploaded as `linux-appimage`.
- [x] **First-Run Setup Wizard**: `FirstRunSetupWizard.kt` — 5-step wizard (Welcome → System Check → Install → Configure → Done); auto-detects Java/Cargo/agent-core via process exec; runs `cargo install agent-core` with streamed output.
- [x] **Bundled JRE**: `build.gradle.kts` — `jlink` modules list (java.base, java.desktop, java.logging, java.management, java.naming, java.net.http, java.prefs, java.rmi, java.scripting, java.security.jgss, java.sql, java.xml, jdk.unsupported); eliminates "Java not found" on end-user machines.
- [x] **Release Automation**: `.github/workflows/build.yml` — builds DMG (macOS), DEB/RPM (Linux), MSI (Windows); `softprops/action-gh-release@v2` publishes on tag push.
- [x] **Direct File-system Editing**: `DesktopUtils.kt` — `openFileInExternalEditor()` (tries VSCode → Desktop.edit → OS fallback) and `openFileInJetBrains()` (scans PATH for IntelliJ/PyCharm/WebStorm).
- [x] **Voice Waveform Visualization**: `VoiceWaveformPanel.kt` — `VoiceWaveform` composable with sinusoidal bar animation; speed varies by state (LISTENING/SPEAKING/PROCESSING/IDLE); `VoiceWaveformPanel` wraps with status label and mic/stop controls.
- [x] **Multi-Agent Heatmap**: `MultiAgentHeatmap.kt` — color-coded heat grid per agent × round; intensity mapped to token counts; Skia Canvas gradient legend; "Heatmap" tab available via UiSettings.showHeatmap.
- [x] **Session Branching UI**: Visual tree explorer — `SessionTimelinePanel.kt` shows Gantt-style message timing; UiSettings.showTimeline flag; "Timeline" tab.
- [x] **Pinned Context Panel**: `PinnedContextPanel.kt` — shows pinned files, add/remove via path input; stored in `UiSettings.pinnedContextFiles`; total size display; "Pinned" tab in NarrowSidebar.
- [x] **Export to HTML**: Session export generates both `.md` and Catppuccin-Mocha themed `.html` file; HTML has inline styles, code blocks, user/agent bubble layout.
- [x] **Local Model Manager**: `LocalModelManager.kt` — connects to Ollama REST API (`/api/tags`); lists installed models with name/family/size; pull model form with `ollama pull` execution; "Models" tab.
- [x] **Advanced Prompt Library**: `PromptLibrary.kt` — 13 built-in prompts in 6 categories (Coding, Analysis, Creative, Research, Security, Meta); category filter chips; expandable preview; one-click apply sets system prompt; "Prompts" tab in NarrowSidebar.
- [x] **Visual Session Timeline**: `SessionTimelinePanel.kt` — Gantt-style horizontal bars per message; color-coded by type (User/Agent/Tool/Error); time ruler showing session duration; legend.
- [x] **Smart Context Pruning**: `SmartContextPruning.kt` — messages sorted by estimated token cost (length/4); color-coded bars (green/amber/red); checkbox selection; savings counter; `onPruneMessages` callback.
- [x] **Personality Lab**: `PersonalityLab.kt` — 6 built-in personas (Architect, Mentor, Hacker, Researcher, Creative, DevOps); custom persona creation with emoji/tone/prompt editor; one-click apply as system prompt; active persona highlighted.
- [x] **Multi-Modal Export System**: `PdfExportHelper.kt` (desktopMain) — Apache PDFBox 3.0; paginated A4 PDF with title/metadata header, `[USER]`/`[AGENT]` labels, word-wrapped body, automatic page breaks; `expect/actual` pattern (`buildPdfExport`); `exportSession()` now writes `.md` + `.html` + `.pdf` to `~/Exports/`.
