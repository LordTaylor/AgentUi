package com.agentcore.ui.components

// Main top-bar composable for the AgentCore UI.
// Renders the project title, centre search field, and right-side action strip (TopBarActions).
// Responsive layout: three breakpoints control which elements are visible.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    isRightSidebarVisible: Boolean = false,
    onToggleRightSidebar: () -> Unit = {},
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
                TitleSection(projectName = projectName, isWide = isWide)

                Spacer(modifier = Modifier.width(8.dp))

                // ── CENTER: Search bar — grows/shrinks with window ───────────
                SearchBar(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it; onSearch(it) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // ── RIGHT: Actions — items hidden progressively on resize ───
                TopBarActions(
                    autoAccept = autoAccept,
                    onToggleAutoAccept = onToggleAutoAccept,
                    onQuickConnect = onQuickConnect,
                    onDumpDebugLog = onDumpDebugLog,
                    themeMode = themeMode,
                    onToggleTheme = onToggleTheme,
                    showToolOutput = showToolOutput,
                    onToggleToolOutput = onToggleToolOutput,
                    showTokenAnalytics = showTokenAnalytics,
                    onToggleTokenAnalytics = onToggleTokenAnalytics,
                    developerMode = developerMode,
                    onToggleDeveloperMode = onToggleDeveloperMode,
                    onOpenDevOptions = onOpenDevOptions,
                    onToggleWorkflowDialog = onToggleWorkflowDialog,
                    onToggleMemoryPanel = onToggleMemoryPanel,
                    isRightSidebarVisible = isRightSidebarVisible,
                    onToggleRightSidebar = onToggleRightSidebar,
                    onToggleProviderDialog = onToggleProviderDialog,
                    isWide = isWide,
                    isMedium = isMedium,
                    isNarrow = isNarrow,
                )
            }
        }
    }
}

@Composable
private fun TitleSection(projectName: String, isWide: Boolean) {
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
}

@Composable
private fun SearchBar(searchText: String, onSearchTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .widthIn(min = 80.dp, max = 420.dp)
            .height(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                onValueChange = onSearchTextChange,
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
}
