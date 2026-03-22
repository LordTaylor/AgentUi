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
Projekt jest modułowy i wykorzystuje nowoczesne wzorce:
- **MVI (Model-View-Intent)**: Przewidywalne zarządzanie stanem i przepływem danych.
- **Dependency Injection**: **Koin** dla modułowości i testowalności.
- `:core-api`: Modele IPC i klient HTTP (**Ktor**).
- `:shared`: Logika wspólna, Executory (CLI, Stdio, Unix Socket).
- `:composeApp`: Warstwa UI (Compose multiplatform).

## Dystrybucja
Wspierane formaty instalatorów:
- macOS: `.dmg`, `.pkg`
- Windows: `.exe`, `.msi`
- Linux: `.deb`, `.rpm`

## Rozwój
Uruchamianie lokalne: `./gradlew :composeApp:run`
Budowanie paczek: `./gradlew :composeApp:package`
