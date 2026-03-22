package com.agentcore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.connection.ConnectionIntent
import com.agentcore.ui.connection.ConnectionUiState

@Composable
fun ConnectionScreen(
    state: ConnectionUiState,
    onIntent: (ConnectionIntent) -> Unit,
    onConnect: (ConnectionMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF121212), Color(0xFF1E1E1E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).widthIn(max = 600.dp)
        ) {
            Text(
                text = "Connect to Agent Core",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose how you want to interact with the agent",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(48.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConnectionOption(
                        title = "IPC Server",
                        description = "existing background server (HTTP/SSE).",
                        icon = Icons.Default.Settings,
                        isSelected = state.selectedMode == ConnectionMode.IPC,
                        onClick = { onIntent(ConnectionIntent.SelectMode(ConnectionMode.IPC)) },
                        modifier = Modifier.weight(1f)
                    )
                    ConnectionOption(
                        title = "Unix Socket",
                        description = "Native high-speed transport (.sock).",
                        icon = Icons.Default.Settings,
                        isSelected = state.selectedMode == ConnectionMode.UNIX_SOCKET,
                        onClick = { onIntent(ConnectionIntent.SelectMode(ConnectionMode.UNIX_SOCKET)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConnectionOption(
                        title = "STDIO Mode",
                        description = "Persistent local link via process pipes.",
                        icon = Icons.Default.CheckCircle,
                        isSelected = state.selectedMode == ConnectionMode.STDIO,
                        onClick = { onIntent(ConnectionIntent.SelectMode(ConnectionMode.STDIO)) },
                        modifier = Modifier.weight(1f)
                    )
                    ConnectionOption(
                        title = "CLI Direct",
                        description = "One-shot execution of the agent binary.",
                        icon = Icons.Default.Home,
                        isSelected = state.selectedMode == ConnectionMode.CLI,
                        onClick = { onIntent(ConnectionIntent.SelectMode(ConnectionMode.CLI)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { state.selectedMode?.let { onConnect(it) } },
                enabled = state.selectedMode != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3F51B5),
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text(
                    text = "Connect Now",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ConnectionOption(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFF3F51B5) else Color(0xFF333333)
    val backgroundColor = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF121212)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = borderColor, borderWidth = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF3F51B5) else Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
