package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale

// Responsive breakpoints (dp widths of the TopBar itself)
private val BP_WIDE   = 1000.dp  // show everything
private val BP_MEDIUM = 750.dp   // hide Pro badge + quick-connect shortcuts
private val BP_NARROW = 580.dp   // also collapse Dev Mode label + hide debug dump

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    projectName: String,
    onSearch: (String) -> Unit,
    onToggleProviderDialog: () -> Unit,
    autoAccept: Boolean,
    onToggleAutoAccept: () -> Unit,
    onQuickConnect: (String) -> Unit,
    onDumpDebugLog: () -> Unit,
    themeMode: String,
    onToggleTheme: () -> Unit,
    showToolOutput: Boolean = false,
    onToggleToolOutput: () -> Unit = {},
    showTokenAnalytics: Boolean = false,
    onToggleTokenAnalytics: () -> Unit = {},
    developerMode: Boolean = true,
    onToggleDeveloperMode: () -> Unit = {},
    onOpenDevOptions: () -> Unit = {},
    onToggleWorkflowDialog: () -> Unit = {},
    onToggleMemoryPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 1.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableWidth = maxWidth
            val isWide   = availableWidth >= BP_WIDE
            val isMedium = availableWidth >= BP_MEDIUM
            val isNarrow = availableWidth < BP_NARROW

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ── LEFT: Title (fixed, non-shrinking) ──────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    if (isWide) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                "Pro",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ── CENTER: Search bar — grows/shrinks with window ───────────
                Surface(
                    modifier = Modifier
                        .weight(1f)                    // takes all available space
                        .widthIn(min = 80.dp, max = 420.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it; onSearch(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (searchText.isEmpty()) {
                                    Text("Szukaj...", color = Color.Gray, fontSize = 12.sp)
                                }
                                inner()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ── RIGHT: Actions — items hidden progressively on resize ───
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Providers (always visible — primary action)
                    AppTooltip("Zarządzaj dostawcami i modelami") {
                        IconButton(onClick = onToggleProviderDialog, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Cloud, "Providers",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
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

                    VerticalDivider(
                        modifier = Modifier.height(20.dp).padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Auto-Accept toggle (always visible)
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

                    // Dev mode toggle — label hidden on narrow
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

                    VerticalDivider(
                        modifier = Modifier.height(20.dp).padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Theme toggle (always visible)
                    AppTooltip("Przełącz motyw Jasny/Ciemny") {
                        IconButton(onClick = onToggleTheme, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (themeMode == "LIGHT") Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle Theme",
                                modifier = Modifier.size(18.dp),
                                tint = Color.Gray
                            )
                        }
                    }

                    // Debug dump — hidden on narrow
                    if (!isNarrow) {
                        AppTooltip("Zapisz logi diagnostyczne") {
                            IconButton(onClick = onDumpDebugLog, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.BugReport, "Dump Debug Log",
                                    modifier = Modifier.size(18.dp), tint = Color.Gray
                                )
                            }
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier.height(20.dp).padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Tool output (always)
                    AppTooltip("Tool Output Live-Stream") {
                        IconButton(onClick = onToggleToolOutput, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Terminal, "Tool Output",
                                modifier = Modifier.size(18.dp),
                                tint = if (showToolOutput) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }

                    // Analytics — medium+ only
                    if (isMedium) {
                        AppTooltip("Token Usage Analytics") {
                            IconButton(onClick = onToggleTokenAnalytics, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.BarChart, "Token Analytics",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (showTokenAnalytics) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }

                    // Workflow (always)
                    AppTooltip("Uruchom Workflow Agentów") {
                        IconButton(onClick = onToggleWorkflowDialog, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Hub, "Agent Workflow",
                                modifier = Modifier.size(18.dp), tint = Color.Gray
                            )
                        }
                    }

                    // Memory (always)
                    AppTooltip("Pamięć Agenta") {
                        IconButton(onClick = onToggleMemoryPanel, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Storage, "Agent Memory",
                                modifier = Modifier.size(18.dp), tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
