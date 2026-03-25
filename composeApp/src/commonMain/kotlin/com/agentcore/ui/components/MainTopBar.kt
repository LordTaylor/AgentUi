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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    projectName: String,
    onSearch: (String) -> Unit,
    onToggleLeftSidebar: () -> Unit,
    onToggleRightSidebar: () -> Unit,
    onToggleProviderDialog: () -> Unit,
    isLeftSidebarVisible: Boolean,
    isRightSidebarVisible: Boolean,
    isSkillsVisible: Boolean,
    onToggleSkills: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth().height(64.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "Pro Workspace",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Search Bar
            Surface(
                modifier = Modifier
                    .width(400.dp)
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchText,
                        onValueChange = { 
                            searchText = it
                            onSearch(it)
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (searchText.isEmpty()) {
                                Text("Search project...", color = Color.Gray, fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppTooltip("Zalecane: Zarządzaj dostawcami i modelami") {
                    IconButton(onClick = onToggleProviderDialog, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Cloud, "Providers", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.width(4.dp))

                // Quick Provider Buttons
                AppTooltip("Szybkie połączenie z Ollama") {
                    IconButton(onClick = { onQuickConnect("ollama") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Bolt, "Ollama", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
                AppTooltip("Szybkie połączenie z LM Studio") {
                    IconButton(onClick = { onQuickConnect("lmstudio") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Terminal, "LM Studio", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
                
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Auto-Accept Toggle
                AppTooltip(if (autoAccept) "Automatyczne zatwierdzanie narzędzi (OSZCZĘDZA CZAS)" else "Ręczne zatwierdzanie narzędzi (BEZPIECZNIEJSZE)") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (autoAccept) Icons.Default.VerifiedUser else Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (autoAccept) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = autoAccept,
                            onCheckedChange = { onToggleAutoAccept() },
                            modifier = Modifier.scale(0.6f)
                        )
                    }
                }

                // Developer Mode Toggle (Option 4)
                AppTooltip(if (developerMode) "Tryb Deweloperski: Pełna transparentność (wszystkie kroki agenta)" else "Tryb Skrócony: Tylko najważniejsze informacje i wyniki") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (developerMode) Icons.Default.Code else Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (developerMode) MaterialTheme.colorScheme.secondary else Color.Gray
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = developerMode,
                            onCheckedChange = { onToggleDeveloperMode() },
                            modifier = Modifier.scale(0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                AppTooltip("Przełącz motyw Jasny/Ciemny") {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (themeMode == "LIGHT") Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Toggle Theme",
                            tint = Color.Gray
                        )
                    }
                }
                AppTooltip("Zapisz logi diagnostyczne") {
                    IconButton(onClick = onDumpDebugLog) {
                        Icon(Icons.Default.BugReport, contentDescription = "Dump Debug Log", tint = Color.Gray)
                    }
                }
                AppTooltip("Powiadomienia") {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.Gray)
                    }
                }
                AppTooltip("Lista sesji (Cmd+B)") {
                    IconButton(onClick = onToggleLeftSidebar) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = "Toggle Sessions",
                            tint = if (isLeftSidebarVisible) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
                AppTooltip("Biblioteka Skilli (Narzędzia)") {
                    IconButton(onClick = onToggleSkills) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Toggle Skills",
                            tint = if (isSkillsVisible) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
                AppTooltip("Eksplorator plików") {
                    IconButton(onClick = onToggleRightSidebar) {
                        Icon(
                            Icons.Default.VerticalSplit,
                            contentDescription = "Toggle File Tree",
                            tint = if (isRightSidebarVisible) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
                
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                AppTooltip("Tool Output Live-Stream") {
                    IconButton(onClick = onToggleToolOutput) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "Tool Output",
                            tint = if (showToolOutput) Color.Green else Color.Gray
                        )
                    }
                }
                
                AppTooltip("Token Usage Analytics") {
                    IconButton(onClick = onToggleTokenAnalytics) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "Token Analytics",
                            tint = if (showTokenAnalytics) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// End of file
