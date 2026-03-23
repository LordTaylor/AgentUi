# AgentCore UI — Claude Code Instructions

## Dokumentacja backendu

Dokumentacja serwera Rust (`agent-core`) znajduje się tutaj:

```
/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp/docs/
├── communication.md   — protokół IPC: komendy, eventy, tryby transportu, formaty JSON
├── ipc-schema.json    — pełny JSON Schema v1.6 wszystkich komend i eventów
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

### REGUŁA 5 — Komendy w pełni zaimplementowane

- `cancel` — działa we wszystkich trybach (IPC, STDIO, UNIX_SOCKET). Backend używa `AtomicBool` cancel token.
- `update_config` — używamy do synchronizacji `approval_mode` po evencie `Ready` (`syncApprovalMode()` w ChatViewModel).

### REGUŁA 6 — Walidacja `StatusPayload.state`

Backend emituje 5 wartości (lowercase): `idle` | `thinking` | `executing` | `waiting_approval` | `backtracking`
- `backtracking` (A03) → UI mapuje na `"THINKING"` (IpcHandler.kt)
Nasz UI normalnie uppercase (`IDLE`, `THINKING`). Konwersja odbywa się w jednym miejscu:
`IpcHandler` → `onStatusChange(...)` → `statusState`
Wszystkie porównania w UI używają `.uppercase()`. **Nie dodawaj** hardkodowanych literałów lowercase w UI.

### REGUŁA 7 — Wersja protokołu

Aktualnie obsługiwana wersja: **v1.6**
Pole `protocol_version` przychodzi w evencie `message_start`.
Zmiany v1.6 względem v1.5:
- Pole `agent_id: String?` na większości eventów (identyfikuje sub-agenty)
- Event `sub_agent_done` z polem `summary`, `success`
- Event `ready` z `protocol_version: "1.6"`
- `spawn_sub_agent` z polem `role`

### REGUŁA 8 — Zarządzanie `session_id` (B07 fix)

`session_id` jest **emitowany przez backend** w `MessageStart` — NIE generuj go w UI.

**Poprawny flow:**
```
IpcHandler: MessageStart → onSessionStart(event.payload.session_id)
ChatViewModel: currentSessionId = sessionId  (przechowuje w ChatUiState)
Następna komenda: SendMessagePayload(session_id = currentSessionId, ...)
```

Jeśli `currentSessionId == null`, `SendMessagePayload` NIE wysyła `session_id` → backend tworzy nową sesję → **model nie pamięta historii**.
Nie resetuj `currentSessionId` między wiadomościami.

### REGUŁA 9 — Aktualizacja ROADMAP.md

Po wykonaniu każdego zadania zdefiniowanego w `docs/ROADMAP.md`, **obowiązkowo** zaktualizuj plik roadmapy, odznaczając wykonane punkty `[x]`. Jeśli zadanie wprowadziło nową funkcjonalność nieujęto wcześniej, dodaj ją do odpowiedniej fazy.

---

## Zasady architektoniczne projektu

- **Divide and Conquer**: Nie twórz dużych monolitycznych plików. Każdy komponent w osobnym pliku.
- **IPC-First**: Cała komunikacja przez ściśle typowany protokół IPC. Nigdy bezpośrednio.
- `core-api` nie importuje niczego z `composeApp` — zależność jest jednostronna.
- Nowe komponenty UI → `composeApp/src/commonMain/kotlin/com/agentcore/ui/components/`
- Nowe modele → `composeApp/src/commonMain/kotlin/com/agentcore/model/`

---

## REGUŁA 10 — Rozmiar plików (czytelność dla AI i ludzi)

**Maksymalny rozmiar pliku: ~150 linii kodu.**
Jeśli plik przekracza ten limit, podziel go na mniejsze moduły.

### Jak dzielić

| Typ pliku | Strategia podziału |
|-----------|-------------------|
| ViewModel (>150 linii) | Wydziel `*Handlers.kt` (logika obsługi intencji) |
| Screen (>150 linii) | Wydziel sekcje do `*Section.kt` lub `*Panel.kt` |
| IpcHandler (>150 linii) | Wydziel `*EventRouter.kt` per kategoria eventów |
| IpcModels (>150 linii) | Podziel na `*Commands.kt` i `*Events.kt` |

### Dlaczego

Agent AI czyta pliki w całości. Plik >200 linii oznacza:
- Wyższy koszt (więcej tokenów)
- Większe ryzyko pominięcia zależności
- Trudniejsze testy jednostkowe

### Nazewnictwo po podziale

```
ChatViewModel.kt           → logika stanu + publiczne intencje
ChatIntentHandlers.kt      → `fun handle*(intent)` helpers
ChatSessionHandlers.kt     → logika sesji + backend switching
```

---

## REGUŁA 11 — Czytelność dla agenta AI

Każdy plik **musi zaczynać się od komentarza** opisującego jego odpowiedzialność w 1-3 zdaniach:

```kotlin
// Dispatches IPC events from the Rust backend to ViewModel callbacks.
// One function per event category; each branch is max ~10 lines.
// See: CoreApp/docs/communication.md for event protocol.
```

**Zakazy:**
- Nie mieszaj warstw w jednym pliku (np. UI + logika biznesowa)
- Nie umieszczaj stałych konfiguracyjnych rozsianych po różnych plikach — zbierz je w `AppConstants.kt`
- Nie twórz funkcji dłuższych niż ~30 linii — wydziel pomocnicze funkcje prywatne
