# AgentCore UI — Claude Code Instructions

## Dokumentacja backendu

Dokumentacja serwera Rust (`agent-core`) znajduje się tutaj:

```
/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp/docs/
├── communication.md   — protokół IPC: komendy, eventy, tryby transportu, formaty JSON
├── ipc-schema.json    — pełny JSON Schema v1.5 wszystkich komend i eventów
└── APP_MAP.md         — mapa modułów Rust, typy, przepływy danych
```

**Zawsze czytaj te pliki przed każdą zmianą w warstwie komunikacji** (IpcModels, AgentClient, IpcHandler).

---

## Struktura projektu

```
uiKotlin/
├── core-api/          — modele IPC (IpcCommand, IpcEvent, payloady) + AgentClient (HTTP/SSE)
├── shared/            — ConnectionMode, executory (Stdio, UnixSocket, CLI)
└── composeApp/
    ├── logic/IpcHandler.kt   — dispatcher eventów + performSendMessage
    └── ui/                   — ekrany, komponenty
```

Pełna dokumentacja komunikacji UI: `docs/COMMUNICATION.md`

---

## Reguły zgodności protokołu IPC

Przy każdej zmianie kodu komunikacji **obowiązkowo** sprawdź poniższe punkty.

### REGUŁA 1 — Nazwy pól JSON muszą dokładnie zgadzać się z backendem

Sprawdź `communication.md` sekcje 6 i 7. Przykłady poprawnych nazw:
- komenda: `"cmd"` (nie `"command"`, nie `"type"`)
- event: `"event"` (nie `"type"`, nie `"name"`)
- `"send_message"`, `"list_tools"`, `"get_stats"` — snake_case
- `"message_start"`, `"text_delta"`, `"message_end"`, `"tool_call"`, `"tool_result"` — snake_case
- Payload: `"session_id"` (nie `"sessionId"`), `"input_tokens"`, `"output_tokens"`

Zła nazwa = cicha awaria (JSON się nie deserializuje, event trafia do `else -> {}`).

### REGUŁA 2 — Format odpowiedzi POST /command

Backend zwraca:
```json
{ "events": [ {...}, {...} ] }
```
**NIE** zwraca pojedynczego `IpcEvent`. Aktualny `AgentClient.sendCommand()` **nie obsługuje poprawnie** tego formatu — jest to znana rozbieżność (patrz `docs/COMMUNICATION.md` sekcja Rozbieżności).

Przy refaktorze `AgentClient` pamiętaj: dla komend niestrumieniujących należy sparsować `events[last]` lub `events[0]` z tablicy.

### REGUŁA 3 — `sessions_list` payload

Backend (communication.md §7) zwraca **tablicę stringów UUID**:
```json
{ "event": "sessions_list", "payload": { "count": 2, "sessions": ["uuid1", "uuid2"] } }
```
Nasz model `SessionsListPayload` ma `sessions: List<SessionInfo>` — to **rozszerzenie własne UI** (dodane pola `backend`, `role`, `created_at` itp.).
Deserializacja działa dzięki `ignoreUnknownKeys = true` i wartościom domyślnym w `SessionInfo`.
**Nie usuwaj** `ignoreUnknownKeys = true` z konfiguracji Json — zepsuje to wsteczną kompatybilność.

### REGUŁA 4 — Eventy obsługiwane vs ignorowane

Każdy nowy `IpcEvent` w `IpcModels.kt` musi mieć odpowiedni `when` branch w `IpcHandler.handleIpcEvent()`.
Sprawdź tabelę w `docs/COMMUNICATION.md` sekcja "Eventy".
Nowe eventy NIE mogą trafiać do `else -> {}` bez uzasadnienia w komentarzu.

### REGUŁA 5 — Komendy NOT_IMPLEMENTED na backendzie

Poniższe komendy są zdefiniowane w protokole, ale backend zwraca `error: NOT_IMPLEMENTED`:
- `cancel` — UI wysyła, ale backend ignoruje (ustawia tylko lokalnie `statusState = "IDLE"`)
- `update_config` — nie używamy

Nie polegaj na nich do działania aplikacji. Obsługuj odpowiedź `IpcEvent.Error` z kodem `NOT_IMPLEMENTED`.

### REGUŁA 6 — Walidacja `StatusPayload.state`

Backend emituje 5 wartości (lowercase): `idle` | `thinking` | `executing` | `waiting_approval` | `backtracking`
- `backtracking` (A03) → UI mapuje na `"THINKING"` (IpcHandler.kt)
Nasz UI normalnie uppercase (`IDLE`, `THINKING`). Konwersja odbywa się w jednym miejscu:
`IpcHandler` → `onStatusChange(...)` → `statusState`
Wszystkie porównania w UI używają `.uppercase()`. **Nie dodawaj** hardkodowanych literałów lowercase w UI.

### REGUŁA 5 — Status komendy `cancel`

Komenda `cancel` **jest w pełni zaimplementowana w backendzie** (cancel token `AtomicBool`).
UI wysyła ją we wszystkich trybach: IPC, STDIO, UNIX_SOCKET.
Wcześniejsza notatka "backend ignoruje" była błędna — naprawiono w Sprint 6 (B03).

### REGUŁA 7 — Versja protokołu

Aktualnie obsługiwana wersja: **v1.5**
Pole `protocol_version` przychodzi w evencie `message_start`.
Przy zmianie na v1.6+ sprawdź diff w `ipc-schema.json` i zaktualizuj `IpcModels.kt`.

### REGUŁA 9 — Aktualizacja ROADMAP.md

Po wykonaniu każdego zadania zdefiniowanego w `docs/ROADMAP.md`, **obowiązkowo** zaktualizuj plik roadmapy, odznaczając wykonane punkty `[x]`. Jeśli zadanie wprowadziło nową funkcjonalność nieujęto wcześniej, dodaj ją do odpowiedniej fazy.

---

## Zasady architektoniczne projektu

- **Divide and Conquer**: Nie twórz dużych monolitycznych plików. Każdy komponent w osobnym pliku.
- **IPC-First**: Cała komunikacja przez ściśle typowany protokół IPC. Nigdy bezpośrednio.
- `core-api` nie importuje niczego z `composeApp` — zależność jest jednostronna.
- Nowe komponenty UI → `composeApp/src/commonMain/kotlin/com/agentcore/ui/components/`
- Nowe modele → `composeApp/src/commonMain/kotlin/com/agentcore/model/`
