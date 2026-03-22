# AgentCore UI - Dokumentacja

## Przegląd
Aplikacja Desktopowa dla `agent-core` napisana w Kotlin Multiplatform.

## Funkcje
- **IPC/CLI/stdio/Unix Socket**: Pełna obsługa transportów agenta.
- **Session Management**: Zarządzanie historią sesji.
- **Settings**: Konfiguracja modelu i roli w czasie rzeczywistym.
- **Attachments**: Drag & Drop plików do rozmowy.
- **Stats Dashboard**: Monitorowanie zużycia tokenów i kosztów.
- **Tool Explorer**: Przeglądanie dostępnych narzędzi agenta.

## Architektura
Projekt jest modułowy:
- `:core-api`: Modele IPC i klient HTTP.
- `:shared`: Logika wspólna, Executory (CLI, Stdio, Unix Socket).
- `:composeApp`: Warstwa UI (Compose).

## Dystrybucja
Wspierane formaty instalatorów:
- macOS: `.dmg`, `.pkg`
- Windows: `.exe`, `.msi`
- Linux: `.deb`, `.rpm`

## Rozwój
Uruchamianie lokalne: `./gradlew :composeApp:run`
Budowanie paczek: `./gradlew :composeApp:package`
