# AgentCore UI — Dokumentacja komunikacji

> Wersja protokołu backendu: **v1.5**
> Dokumentacja UI: **zaktualizowana do MVI** (2026-03-22)
> Backend docs: `/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp/docs/communication.md`
> Backend directory: `/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp/docs` (Źródło dokumentacji backendowej)

---

## Spis treści

1. [Tryby połączenia](#1-tryby-połączenia)
2. [Warstwa transportu](#2-warstwa-transportu)
3. [Komendy IPC — wysyłane przez UI](#3-komendy-ipc--wysyłane-przez-ui)
4. [Eventy IPC — odbierane przez UI](#4-eventy-ipc--odbierane-przez-ui)
5. [Przepływ danych — pełna wiadomość](#5-przepływ-danych--pełna-wiadomość)
6. [Rozbieżności z dokumentacją backendu](#6-rozbieżności-z-dokumentacją-backendu)
7. [Pliki kluczowe](#7-pliki-kluczowe)

---

## 1. Tryby połączenia

UI obsługuje 4 tryby, wybierane na `ConnectionScreen` przez użytkownika:

| Tryb (`ConnectionMode`) | Klasa transportu | Backend | Streaming |
|-------------------------|-----------------|---------|-----------|
| `IPC` | `AgentClient` (Ktor HTTP) | `--server` na porcie 7700 | SSE (`GET /events`) |
| `STDIO` | `StdioExecutor` | spawned `agent-core --stdio` | JSON Lines stdin/stdout |
| `UNIX_SOCKET` | `UnixSocketExecutor` | `--server` na `~/.agentcore/agent.sock` | JSON Lines |
| `CLI` | `CliExecutor` | jednorazowe wywołanie `agent-core "msg"` | brak (plain text) |

### Zalecenia wyboru trybu (zgodne z backendem)

- **Desktop GUI (produkcja)** → `STDIO` — brak sieci, brak osobnego procesu serwera
- **Wiele okien / debugowanie** → `IPC` — długożyjący serwer, SSE events, REST API
- **Szybki test** → `CLI` — jednorazowe wywołanie, odpowiedź plain text
- **macOS / Linux, daemon** → `UNIX_SOCKET` — high-performance streaming

---

## 2. Warstwa transportu

### 2.1 IPC — `AgentClient` (`core-api/AgentClient.kt`)

Klient HTTP/SSE dla trybu `--server`.

**Endpointy backendu:**

| Metoda | Ścieżka | Użycie w UI |
|--------|---------|-------------|
| `POST` | `/command` | Wysyłanie komend (set_backend, get_stats, get_session itp.) |
| `GET` | `/sessions` | Listowanie sesji przy starcie |
| `GET` | `/events` | SSE stream — główny kanał eventów |

**Metody `AgentClient`:**

| Metoda Kotlin | Komenda IPC | Zwraca |
|---------------|-------------|--------|
| `sendCommand(IpcCommand)` | dowolna | `IpcEvent?` |
| `ping()` | `ping` | `PingResultPayload?` |
| `cancel(sessionId)` | `cancel` | `Boolean` |
| `deleteSession(sessionId)` | `delete_session` | `Boolean` |
| `listSessions()` | `GET /sessions` | `List<SessionInfo>` |
| `getSession(sessionId)` | `get_session` | `SessionDataPayload?` |
| `updateBackend(backend, model)` | `set_backend` | `Boolean` |
| `updateRole(role)` | `set_role` | `Boolean` |
| `getStats()` | `get_stats` | `JsonObject?` |
| `listTools()` | `list_tools` | `List<JsonObject>` |
| `observeEvents()` | SSE stream | `Flow<IpcEvent>` |

**Reconnect SSE:** 3 próby z backoffem 2s → 4s → 8s. Po wyczerpaniu emituje `IpcEvent.Error`.

**Timeout:** 30s request, 10s connect.

**URL:** Hardkodowany `http://localhost:7700` — konfigurowalność planowana w Phase 8.

---

### 2.2 STDIO — `StdioExecutor`

Spawna `agent-core --stdio`. Komunikacja przez potoki:
- **stdin** → komendy JSON (jedna linia)
- **stdout** → eventy JSON (streaming)

Inicjalizacja: `stdioExecutor.start()` → czeka na `{"event":"ready",...}`.

Wysyłanie: `stdioExecutor.sendCommand(IpcCommand)` → serializuje do JSON + `\n` na stdin.

---

### 2.3 UNIX_SOCKET — `UnixSocketExecutor`

Łączy się do `~/.agentcore/agent.sock`. Newline-delimited JSON.
Wymaga wcześniej uruchomionego `agent-core --server`.
Native Java 17 `SocketChannel`.

---

### 2.4 CLI — `CliExecutor`

Jednorazowe wywołanie: `agent-core "<text>"`.
Odpowiedź plain text (nie JSON).
Używany tylko w trybie `CLI`, wynik trafia bezpośrednio jako wiadomość agenta.

---

## 3. Komendy IPC — wysyłane przez UI

Wszystkie komendy są zdefiniowane w `core-api/IpcModels.kt` jako `sealed class IpcCommand`.
Format JSON: `{"cmd":"<nazwa>","payload":{...}}`.

### Komendy aktywnie używane przez UI

| Komenda (`cmd`) | Kiedy wysyłana | Kotlin klasa |
|-----------------|---------------|--------------|
| `send_message` | Użytkownik wysyła wiadomość | `IpcCommand.SendMessage` |
| `get_session` | Kliknięcie sesji w sidebarze | `IpcCommand.GetSession` |
| `set_backend` | Zapis w SettingsDialog | `IpcCommand.SetBackend` |
| `set_role` | Zapis w SettingsDialog | `IpcCommand.SetRole` |
| `get_stats` | Kliknięcie ikony Stats | `IpcCommand.GetStats` |
| `list_tools` | Inicjalizacja w trybie IPC | `IpcCommand.ListTools` |
| `approval_response` | Zatwierdzenie/odrzucenie narzędzia | `IpcCommand.ApprovalResponse` |
| `cancel` | Przycisk Cancel (THINKING) | `IpcCommand.Cancel` ✅ |
| `ping` | AgentClient.ping() | `IpcCommand.Ping` |
| `delete_session` | AgentClient.deleteSession() | `IpcCommand.DeleteSession` |
| `fork_session` | Kliknięcie ikony "Fork" (Branch) przy wiadomości | `IpcCommand.ForkSession` |
| `dump_debug_log` | Kliknięcie ikony Bug w TopBarze | `IpcCommand.DumpDebugLog` |
| `update_config` | Synchronizacja `approval_mode` | `IpcCommand.UpdateConfig` |

> ✅ `cancel` — zaimplementowany w CoreApp (commit `5fc2fc2`). Backend ustawia `AtomicBool`
> cancel token + przerywa task cooperative na granicy iteracji pętli agenta.
> UI może usunąć lokalny fallback `statusState = "IDLE"` i polegać na zdarzeniu `message_end`.

### Payload `send_message` — pełna struktura

```json
{
  "cmd": "send_message",
  "payload": {
    "session_id": "550e8400-...",
    "text": "napisz testy dla src/lib.rs",
    "attachments": ["src/lib.rs"],
    "include_stats": false,
    "images": null
  }
}
```

UI wysyła obecnie `include_stats: false` i `images: null`. Pole `images` gotowe w modelu,
obsługa w UI planowana w Phase 9 (Vision Input).

### Komendy zdefiniowane w modelu, nieużywane przez UI

| Komenda | Powód |
|---------|-------|
| `update_scratchpad` | Scratchpad zapisywany lokalnie, sync niezaimplementowany |
| `start_indexing` / `get_indexing_status` | UI pokazuje panel, ale nie inicjuje indeksowania |
| `list_plugins` / `enable_plugin` / `disable_plugin` | Panel PluginManager tylko wyświetla, nie steruje |
| `list_workflows` / `start_workflow` / `stop_workflow` | WorkflowBuilder tylko wyświetla |
| `fork_session` | Planowane Phase 9 |
| `get_config` / `set_system_prompt` / `reload_tools` | Planowane Phase 8 |
| `schedule_task` / `cancel_scheduled_task` / `list_scheduled_tasks` | Planowane Phase 8 |

---

## 4. Eventy IPC — odbierane przez UI

Eventy przychodzą przez SSE (`IpcMode.IPC`), stdout (`STDIO`), lub socket (`UNIX_SOCKET`).
Handler: `IpcHandler.handleIpcEvent()` (Functional Registry) wywoływany przez `ChatViewModel`.

### Eventy w pełni obsługiwane ✅

| Event (`event`) | Handler w IpcHandler | Efekt w UI |
|-----------------|---------------------|------------|
| `status` | `onStatusChange(state)` | Zmiana statusu w headerze (IDLE/THINKING/EXECUTING) |
| `message_start` | `onStatusChange("THINKING")` | Natychmiastowy feedback przed pierwszym tokenem |
| `text_delta` | append/create Message | Streaming tekstu w chacie — łączy tokeny |
| `message_end` | `onStatusChange("IDLE")` | Koniec odpowiedzi agenta |
| `stats` | `onStatsUpdate` | Aktualizacja StatsDashboard i TokenTracker |
| `approval_request` | `onApprovalRequest` | Otwiera ApprovalDialog (modal overlay) |
| `error` | Add SYSTEM Message + `onStatusChange("IDLE")` | Błąd widoczny w chacie jako `❌ [KOD] treść` |
| `tool_call` | Add ACTION Message | `⚙️ tool_name(args)` w chacie |
| `tool_result` | Add ACTION Message | `✅ wynik` lub `❌ błąd` w chacie |
| `thought` | Add SYSTEM Message | `💭 tekst` — bloki ReAct widoczne w chacie |
| `log` | `onLogReceived` | Dodaje do LogViewer |
| `scratchpad_data` | `onScratchpadUpdate` | Aktualizuje Scratchpad |
| `terminal_traffic` | `onTerminalTraffic` | Dodaje do TerminalViewer |
| `indexing_progress` | `onIndexingProgress` | Callback gotowy (panel pomijany tymczasowo) |
| `plugin_metadata` | `onPluginsLoaded` | Aktualizuje PluginManager |
| `workflow_status` | `onWorkflowsUpdate` | Aktualizuje WorkflowBuilder |
| `voice_status` | `onVoiceUpdate` | Callback gotowy (UI niezaimplementowane) |
| `context_suggestions` | `onContextSuggestions` | Aktualizuje PredictiveContext |
| `session_data` | `onSessionData` | Callback gotowy (metadane sesji) |
| `human_input_request` | `onHumanInputRequest` | Otwiera HumanInputDialog |
| `agent_group_update` | `onAgentGroupUpdate` | Aktualizuje AgentOrchestrator |
| `session_forked` | `onSessionForked` | Automatyczne przełączenie na nową odnogę sesji |
| `session_started` | `onSessionStart` | Powiązanie UI z aktualnym UUID sesji i auto-zapis |

### Eventy odbierane, niezaimplementowane w UI (trafiają do `else -> {}`)

| Event | Co robi backend | Plan |
|-------|----------------|------|
| `message_start` | Zwraca `session_id`, `message_id`, `protocol_version` | Wersja protokołu logowana w przyszłości |
| `sessions_list` | Zwracany przez REST `/sessions` w IPC mode | Już obsługiwany przez REST, nie SSE |
| `tools_list` | Zwracany przez REST `list_tools` | Już obsługiwany przez REST |
| `tool_progress` | Streaming wyjścia narzędzia linia po linii | Phase 9 — live tool cards |
| `tool_created` | Nowe narzędzie zapisane przez agenta | Phase 9 — toast notification |
| `voice_transcription` | Transkrypcja STT | Phase 9 — voice panel |
| `canvas_update` | Aktualizacja elementów canvas | Phase 9 — canvas integration |
| `ping_result` | Wersja i uptime serwera | Phase 8 — connection status bar |
| `backends_list` | Lista dostępnych backendów | Phase 8 — Settings dropdown |
| `task_scheduled` | Potwierdzenie zadania cron | Phase 8 — WorkflowBuilder |
| `config` | Konfiguracja serwera | Phase 8 — Settings pre-fill |

---

## 5. Przepływ danych — pełna wiadomość

### Tryb IPC (HTTP + SSE)

```
Użytkownik wpisuje tekst → Button "Send"
  │
  ▼
IpcHandler.performSendMessage()
  │  Dodaje Message(isFromUser=true) do listy
  │  statusState = "THINKING"
  │
  ▼
AgentClient.sendCommand(IpcCommand.SendMessage)
  │  POST http://localhost:7700/command
  │  Body: {"cmd":"send_message","payload":{...}}
  │
  ▼ (równolegle przez SSE)
AgentClient.observeEvents() → Flow<IpcEvent>
  │
  ├─ IpcEvent.MessageStart  → statusState = "THINKING"
  ├─ IpcEvent.TextDelta     → append tekstu do ostatniej wiadomości
  ├─ IpcEvent.ToolCall      → dodaj ACTION message "⚙️ ..."
  ├─ IpcEvent.ToolResult    → dodaj ACTION message "✅ ..."
  ├─ IpcEvent.ApprovalRequest → pendingApproval = payload → ApprovalDialog
  ├─ IpcEvent.HumanInputRequest → pendingHumanInput = payload → HumanInputDialog
  └─ IpcEvent.MessageEnd    → statusState = "IDLE"
```

### Tryb STDIO

```
Użytkownik wpisuje tekst → Button "Send"
  │
  ▼
IpcHandler.performSendMessage()
  │
  ▼
StdioExecutor.sendCommand()
  │  Serializuje IpcCommand do JSON
  │  Zapisuje linię na stdin procesu agent-core
  │
  ▼ (przez stdout)
StdioExecutor.events: Flow<IpcEvent>
  │  Parsuje JSON Lines ze stdout
  │  (taki sam przepływ jak SSE)
```

### Anulowanie zadania

```
Użytkownik klika Cancel (przycisk widoczny gdy THINKING)
  │
  ▼
ChatMainScreen.onCancel()
  │  AgentClient.sendCommand(IpcCommand.Cancel(session_id))
  │  ✅ Backend ustawia AtomicBool cancel token
  │  Agent przerywa na granicy iteracji (cooperative cancellation)
  │  Backend emituje message_end
  │  statusState = "IDLE"  ← ustawiany przez message_end
```

---

## 6. Rozbieżności z dokumentacją backendu

> Sprawdzano względem: `CoreApp/docs/communication.md` i `CoreApp/docs/ipc-schema.json`

### 🔴 Krytyczne (mogą powodować runtime failures)

| # | Problem | Backend mówi | UI robi | Ryzyko |
|---|---------|-------------|---------|--------|
| 1 | **Format odpowiedzi POST /command** | `{"events":[{...},{...}]}` (tablica) | `AgentClient.sendCommand()` parsuje jako pojedynczy `IpcEvent` | Komendy HTTP zwracają null zamiast odpowiedzi |
| 2 | **`cancel` NAPRAWIONY** | Cooperative AtomicBool token (commit `5fc2fc2`) | UI wysyła i oczekuje `message_end` | ✅ Agent jest przerywany na granicy iteracji |

### 🟡 Rozbieżności modelu (działają dzięki `ignoreUnknownKeys`)

| # | Problem | Backend mówi | UI robi |
|---|---------|-------------|---------|
| 3 | **`sessions_list` payload** | `"sessions": ["uuid1", "uuid2"]` — tablica stringów | `"sessions": List<SessionInfo>` — rozszerzone obiekty |
| 4 | **`session_data` payload** | Tylko `session_id`, `message_count`, `role` | UI dodaje `backend`, `tags`, `created_at`, `updated_at` |
| 5 | **`StatusPayload`** | Może zawierać `role` i `backend` pola | Nasz model ma tylko `state: String` |

> Rozbieżności 3–5 nie powodują błędów dzięki `ignoreUnknownKeys = true` i wartościom domyślnym.
> Ryzyko: UI nie widzi pól `role`/`backend` ze `status` eventu, nie aktualizuje nagłówka.

### 🟢 Rozszerzenia UI ponad protokół (celowe, bezpieczne)

| Rozszerzenie | Opis |
|-------------|------|
| `SessionInfo` z metadanymi | Sidebar pokazuje `backend`, `role`, `message_count`, `created_at` |
| `IpcEvent` — 31 eventów vs 17 w backendzie | UI gotowe na przyszłe eventy |
| `IpcCommand` — 35 komend | Wiele planowanych funkcji (Phase 8/9) |
| SSE reconnect z backoffem | Backend nie wymaga, UI dodaje dla UX |

---

## 7. Pliki kluczowe

| Plik | Odpowiedzialność |
|------|-----------------|
| `core-api/IpcModels.kt` | Wszystkie typy protokołu: IpcCommand, IpcEvent, payloady |
| `core-api/AgentClient.kt` | HTTP/SSE klient — REST calls + SSE stream + reconnect |
| `shared/Models.kt` | `enum class ConnectionMode` — 4 tryby |
| `composeApp/logic/IpcHandler.kt` | Functional Registry — mapuje eventy na callbacki stanu |
| `composeApp/ui/chat/ChatViewModel.kt` | **MVI ViewModel** — trzyma `uiState`, obsługuje `intent`, zarządza eventami IPC |
| `composeApp/ui/chat/ChatMvi.kt` | Definicje `ChatUiState` oraz `ChatIntent` |
| `composeApp/ui/ChatMainScreen.kt` | Stateless UI — wstrzykuje ViewModel, obserwuje stan, emituje intenty |
| `composeApp/di/AppModule.kt` | Konfiguracja **Koin** — wstrzykiwanie zależności |
| `composeApp/ui/MainScreen.kt` | Główny layout, przekazuje stan do granularnych komponentów |
| `composeApp/ui/components/Sidebar.kt` | Lista sesji z metadanymi SessionInfo |
| `composeApp/ui/components/ApprovalDialog.kt` | Modal zatwierdzenia narzędzia |
| `composeApp/ui/components/HumanInputDialog.kt` | Modal odpowiedzi na pytanie agenta |

### Backend docs (źródło prawdy)

| Plik | Zawartość |
|------|-----------|
| `CoreApp/docs/communication.md` | Protokół IPC v1.5 — komendy, eventy, formaty JSON, przykłady |
| `CoreApp/docs/ipc-schema.json` | Pełny JSON Schema — wszystkie pola, typy, enum wartości |
| `CoreApp/docs/APP_MAP.md` | Mapa modułów Rust, typy, przepływy danych |
