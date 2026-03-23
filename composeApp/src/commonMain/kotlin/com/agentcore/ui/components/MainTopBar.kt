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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    projectName: String,
    onSearch: (String) -> Unit,
    onToggleLeftSidebar: () -> Unit,
    onToggleRightSidebar: () -> Unit,
    isLeftSidebarVisible: Boolean,
    isRightSidebarVisible: Boolean,
    isToolsVisible: Boolean,
    onToggleTools: () -> Unit,
    autoAccept: Boolean,
    onToggleAutoAccept: () -> Unit,
    onQuickConnect: (String) -> Unit,
    cauldronState: CauldronState = CauldronState.IDLE,
    themeMode: String,
    onToggleTheme: () -> Unit,
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
                Spacer(modifier = Modifier.width(12.dp))
                WitchCauldron(
                    state = cauldronState,
                    modifier = Modifier.size(40.dp)
                )
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
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
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
                // Quick Provider Buttons
                IconButton(onClick = { onQuickConnect("ollama") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CloudQueue, "Ollama", modifier = Modifier.size(18.dp), tint = Color.Gray)
                }
                IconButton(onClick = { onQuickConnect("lmstudio") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Hub, "LM Studio", modifier = Modifier.size(18.dp), tint = Color.Gray)
                }
                
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Auto-Accept Toggle
                IconButton(
                    onClick = onToggleAutoAccept,
                    modifier = Modifier.size(32.dp).background(
                        if (autoAccept) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        imageVector = if (autoAccept) Icons.Default.VerifiedUser else Icons.Default.Shield,
                        contentDescription = "Auto-Accept",
                        modifier = Modifier.size(20.dp),
                        tint = if (autoAccept) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (themeMode == "LIGHT") Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Toggle Theme",
                        tint = Color.Gray
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.Gray)
                }
                IconButton(onClick = onToggleLeftSidebar) {
                    Icon(
                        Icons.Default.Dashboard,
                        contentDescription = "Toggle Sessions",
                        tint = if (isLeftSidebarVisible) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                IconButton(onClick = onToggleTools) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Toggle Tools",
                        tint = if (isToolsVisible) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                IconButton(onClick = onToggleRightSidebar) {
                    Icon(
                        Icons.Default.VerticalSplit,
                        contentDescription = "Toggle File Tree",
                        tint = if (isRightSidebarVisible) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

// End of file
