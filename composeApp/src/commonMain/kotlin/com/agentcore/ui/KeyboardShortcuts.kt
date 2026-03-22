package com.agentcore.ui

import androidx.compose.ui.input.key.*

data class AppShortcuts(
    val onNewSession: () -> Unit,
    val onClearChat: () -> Unit,
    val onToggleSettings: () -> Unit,
    val onToggleSidebar: () -> Unit,
    val onFocusInput: () -> Unit
)

fun KeyEvent.matchesShortcut(key: Key, ctrl: Boolean = true, shift: Boolean = false): Boolean =
    type == KeyEventType.KeyDown &&
    isCtrlPressed == ctrl &&
    isShiftPressed == shift &&
    this.key == key

fun handleKeyboardShortcut(event: KeyEvent, shortcuts: AppShortcuts): Boolean {
    return when {
        event.matchesShortcut(Key.K) -> { shortcuts.onNewSession(); true }
        event.matchesShortcut(Key.L) -> { shortcuts.onClearChat(); true }
        event.matchesShortcut(Key.Comma) -> { shortcuts.onToggleSettings(); true }
        event.matchesShortcut(Key.B) -> { shortcuts.onToggleSidebar(); true }
        else -> false
    }
}
