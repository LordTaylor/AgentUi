# Agent Auto-Accept Mechanism Documentation

## Overview
The Auto-Accept mechanism allows the agent to execute tools and actions without requiring manual user confirmation for every step. This is designed to speed up workflows at the cost of reduced oversight.

## Components

### 1. UI Layer (`MainTopBar.kt`)
- Provides a toggle switch with a "VerifiedUser" icon.
- Tooltip: "Automatyczne zatwierdzanie narzędzi (OSZCZĘDZA CZAS)".

### 2. Logic Layer (`ChatViewModel.kt`)
- **State Management**: Uses `uiSettings.autoAccept`.
- **Synchronization**: `syncApprovalMode()` sends the configuration to the backend.
- **Config Mapping**:
    - `autoAccept: true` (UI) -> `approval_mode: false` (Backend)
    - `autoAccept: true` (UI) -> `plan_before_act: false` (Backend)
    - `autoAccept: true` (UI) -> `confidence_gate_approval: false` (Backend)

### 3. IPC Layer (`IpcModels.kt`)
- Uses `IpcCommand.UpdateConfig` with `UpdateConfigPayload` to notify the backend of mode changes.

## Data Flow
1. User toggles switch in `MainTopBar`.
2. `ChatViewModel` receives `ChatIntent.UpdateUiSettings`.
3. `ChatViewModel` updates local state and persists it.
4. `ChatViewModel` emits three `UpdateConfig` commands via IPC to the backend.
5. Backend (agent-core) adjusts its execution loop to skip confirmation states.

## Security Warning
When Auto-Accept is enabled, the agent can perform destructive operations (file deletion, shell commands) without a "Check" step. It is recommended to use this mode only in trusted environments or on non-critical projects.
