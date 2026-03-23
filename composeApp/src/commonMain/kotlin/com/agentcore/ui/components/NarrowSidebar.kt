package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NarrowSidebar(
    activeTab: String,
    onTabSelect: (String) -> Unit,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(64.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Logo / App Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Adb, contentDescription = null, tint = Color.White)
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // New Session Button
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onNewSession() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Session",
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Tabs
                NavIcon(Icons.Default.Chat, "Chat", activeTab == "Chat") { onTabSelect("Chat") }
                NavIcon(Icons.Default.History, "History", activeTab == "History") { onTabSelect("History") }
                NavIcon(Icons.Default.AutoStories, "Library", activeTab == "Library") { onTabSelect("Library") }
                NavIcon(Icons.Default.Science, "Models", activeTab == "Models") { onTabSelect("Models") }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NavIcon(Icons.Default.Settings, "Settings", activeTab == "Settings") { onTabSelect("Settings") }
                Spacer(modifier = Modifier.height(8.dp))
                // User Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Gray, RoundedCornerShape(16.dp))
                )
            }
        }
    }
}

@Composable
private fun NavIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    AppTooltip(label) {
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(48.dp)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
            }
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
