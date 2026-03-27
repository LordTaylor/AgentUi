package com.agentcore.ui.components

// Stateless action-button strip rendered on the right side of MainTopBar.
// Each button group is isolated; visibility is controlled by isWide/isMedium/isNarrow flags.
// No state is held here — all callbacks are passed from MainTopBar.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun TopBarDivider() {
    VerticalDivider(
        modifier = Modifier.height(20.dp).padding(horizontal = 2.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
}

@Composable
internal fun TopBarActions(
    autoAccept: Boolean,
    onToggleAutoAccept: () -> Unit,
    onQuickConnect: (String) -> Unit,
    onDumpDebugLog: () -> Unit,
    themeMode: String,
    onToggleTheme: () -> Unit,
    showToolOutput: Boolean,
    onToggleToolOutput: () -> Unit,
    showTokenAnalytics: Boolean,
    onToggleTokenAnalytics: () -> Unit,
    developerMode: Boolean,
    onToggleDeveloperMode: () -> Unit,
    onOpenDevOptions: () -> Unit,
    onToggleWorkflowDialog: () -> Unit,
    onToggleMemoryPanel: () -> Unit,
    isRightSidebarVisible: Boolean,
    onToggleRightSidebar: () -> Unit,
    onToggleProviderDialog: () -> Unit,
    isWide: Boolean,
    isMedium: Boolean,
    isNarrow: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Providers (always visible — primary action)
        AppTooltip("Zarządzaj dostawcami i modelami") {
            IconButton(onClick = onToggleProviderDialog, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Cloud, "Providers", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        // Quick connects — only on medium+ width
        if (isMedium) {
            AppTooltip("Szybkie połączenie z Ollama") {
                IconButton(onClick = { onQuickConnect("ollama") }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Bolt, "Ollama", modifier = Modifier.size(14.dp), tint = Color.Gray)
                }
            }
            AppTooltip("Szybkie połączenie z LM Studio") {
                IconButton(onClick = { onQuickConnect("lmstudio") }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Terminal, "LM Studio", modifier = Modifier.size(14.dp), tint = Color.Gray)
                }
            }
        }
        TopBarDivider()
        AutoAcceptToggle(autoAccept = autoAccept, onToggleAutoAccept = onToggleAutoAccept)
        DevModeToggle(developerMode = developerMode, isNarrow = isNarrow,
            onToggleDeveloperMode = onToggleDeveloperMode, onOpenDevOptions = onOpenDevOptions)
        TopBarDivider()
        // Theme toggle (always visible)
        AppTooltip("Przełącz motyw Jasny/Ciemny") {
            IconButton(onClick = onToggleTheme, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (themeMode == "LIGHT") Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = "Toggle Theme", modifier = Modifier.size(18.dp), tint = Color.Gray
                )
            }
        }
        // Debug dump — hidden on narrow
        if (!isNarrow) {
            AppTooltip("Zapisz logi diagnostyczne") {
                IconButton(onClick = onDumpDebugLog, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.BugReport, "Dump Debug Log", modifier = Modifier.size(18.dp), tint = Color.Gray)
                }
            }
        }
        // File tree toggle (always visible)
        AppTooltip("Eksplorator plików") {
            IconButton(onClick = onToggleRightSidebar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.VerticalSplit, "Eksplorator plików", modifier = Modifier.size(18.dp),
                    tint = if (isRightSidebarVisible) MaterialTheme.colorScheme.primary else Color.Gray)
            }
        }
        TopBarDivider()
        // Tool output (always)
        AppTooltip("Tool Output Live-Stream") {
            IconButton(onClick = onToggleToolOutput, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Terminal, "Tool Output", modifier = Modifier.size(18.dp),
                    tint = if (showToolOutput) Color(0xFF4CAF50) else Color.Gray)
            }
        }
        // Analytics — medium+ only
        if (isMedium) {
            AppTooltip("Token Usage Analytics") {
                IconButton(onClick = onToggleTokenAnalytics, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.BarChart, "Token Analytics", modifier = Modifier.size(18.dp),
                        tint = if (showTokenAnalytics) MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
        }
        // Workflow (always)
        AppTooltip("Uruchom Workflow Agentów") {
            IconButton(onClick = onToggleWorkflowDialog, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Hub, "Agent Workflow", modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
        // Memory (always)
        AppTooltip("Pamięć Agenta") {
            IconButton(onClick = onToggleMemoryPanel, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Storage, "Agent Memory", modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
private fun AutoAcceptToggle(autoAccept: Boolean, onToggleAutoAccept: () -> Unit) {
    AppTooltip(
        if (autoAccept) "Auto-approve: WŁĄCZONE (szybciej)"
        else "Auto-approve: WYŁĄCZONE (bezpieczniej)"
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Icon(
                imageVector = if (autoAccept) Icons.Default.VerifiedUser else Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (autoAccept) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Switch(
                checked = autoAccept,
                onCheckedChange = { onToggleAutoAccept() },
                modifier = Modifier.scale(0.55f)
            )
        }
    }
}

@Composable
private fun DevModeToggle(
    developerMode: Boolean,
    isNarrow: Boolean,
    onToggleDeveloperMode: () -> Unit,
    onOpenDevOptions: () -> Unit
) {
    AppTooltip(if (developerMode) "Dev: widoczne kroki agenta" else "Clean: tylko rozmowa") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    if (developerMode) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Icon(
                imageVector = if (developerMode) Icons.Default.BugReport else Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (developerMode) MaterialTheme.colorScheme.secondary else Color.Gray
            )
            if (!isNarrow) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = if (developerMode) "DEV" else "CLEAN",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (developerMode) MaterialTheme.colorScheme.secondary else Color.Gray
                )
            }
            Switch(
                checked = developerMode,
                onCheckedChange = { onToggleDeveloperMode() },
                modifier = Modifier.scale(0.55f)
            )
            if (developerMode && !isNarrow) {
                IconButton(onClick = onOpenDevOptions, modifier = Modifier.size(18.dp)) {
                    Icon(
                        Icons.Default.Settings, "Opcje trybu dev",
                        Modifier.size(11.dp),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
