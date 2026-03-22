package com.agentcore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.connection.ConnectionIntent
import com.agentcore.ui.connection.ConnectionUiState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Premium Dark Theme Palette
private val SurfaceDark = Color(0xFF0D0E14)
private val SidebarDark = Color(0xFF14151B)
private val CardDark = Color(0xFF1A1B22)
private val AccentPurple = Color(0xFFB283FF)
private val AccentTeal = Color(0xFF4FD1C5)
private val TextSecondary = Color(0xFF94959B)

@Composable
fun ConnectionScreen(
    state: ConnectionUiState,
    onIntent: (ConnectionIntent) -> Unit,
    onConnect: (ConnectionMode) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(SurfaceDark)) {
        val isSmallScreen = maxWidth < 800.dp
        val cardColumns = when {
            maxWidth < 700.dp -> 1
            maxWidth < 1100.dp -> 2
            else -> 4
        }
        val contentPadding = if (isSmallScreen) 16.dp else 48.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // --- Sidebar (Hidden on small screens) ---
            if (!isSmallScreen) {
                Sidebar(modifier = Modifier.width(280.dp).fillMaxHeight())
            }

            // --- Main Content ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
            ) {
                Header(isSmallScreen = isSmallScreen)

                Spacer(modifier = Modifier.height(64.dp))

                // Hero Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Connect to ")
                            withStyle(style = SpanStyle(color = AccentPurple)) {
                                append("Agent Core")
                            }
                        },
                        style = if (isSmallScreen) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Choose how you want to interact with the agent. Professional-grade transport protocols for high-fidelity digital architecture.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(64.dp))

                // Connection Cards Grid (Adaptive)
                ConnectionCardsGrid(
                    columns = cardColumns,
                    onConnect = onConnect
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ConnectionCardsGrid(
    columns: Int,
    onConnect: (ConnectionMode) -> Unit
) {
    val cards = listOf(
        Triple("IPC Server", "Connect via background server using HTTP or SSE protocols. Ideal for web-integrated workflows.", ConnectionMode.IPC to Icons.Default.Settings),
        Triple("Unix Socket", "Native high-speed transport via .sock file. Minimum latency for local system operations.", ConnectionMode.UNIX_SOCKET to Icons.Default.Build),
        Triple("STDIO Mode", "Persistent local link via process pipes. Reliable, secure, and compatible with most IDEs.", ConnectionMode.STDIO to Icons.Default.Check),
        Triple("CLI Direct", "One-shot execution of the agent binary. Best for automated scripts and quick queries.", ConnectionMode.CLI to Icons.Default.PlayArrow)
    )

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        for (i in cards.indices step columns) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                for (j in 0 until columns) {
                    if (i + j < cards.size) {
                        val (title, desc, modeIcon) = cards[i + j]
                        ConnectionCard(
                            title = title,
                            description = desc,
                            icon = modeIcon.second,
                            btnText = when(modeIcon.first) {
                                ConnectionMode.IPC -> "INITIALIZE TRANSPORT"
                                ConnectionMode.UNIX_SOCKET -> "MOUNT SOCKET"
                                ConnectionMode.STDIO -> "ESTABLISH PIPE"
                                else -> "EXECUTE BINARY"
                            },
                            onClick = { onConnect(modeIcon.first) },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (columns > 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SidebarDark)
            .padding(24.dp)
    ) {
        // Logo
        Column {
            Text(
                "Digital Architect",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                "PROFESSIONAL SUITE",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // New Session Button
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Session", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Nav Menu
        NavItem(Icons.Default.Email, "Conversations")
        NavItem(Icons.Default.Home, "Architectures")
        NavItem(Icons.Default.Menu, "Knowledge Base")

        Spacer(modifier = Modifier.weight(1f))

        // Footer Actions
        NavItem(Icons.Default.Settings, "Settings")
        NavItem(Icons.Default.Info, "Support")
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun Header(isSmallScreen: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isSmallScreen) {
                Text("SYSTEM STATUS: ", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentTeal.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentTeal))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CORE READY", color = AccentTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            if (!isSmallScreen) {
                Spacer(modifier = Modifier.width(20.dp))
                Icon(Icons.Default.Star, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(24.dp))
            
            if (!isSmallScreen) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("ADMIN_ROOT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("v2.4.0-STABLE", color = TextSecondary, fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AccentPurple.copy(alpha = 0.2f))
                    .border(1.dp, AccentPurple.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    btnText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(380.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Color.White.copy(alpha = 0.05f), borderWidth = 1.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(description, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            
            Spacer(modifier = Modifier.weight(1f))
            
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Color.White.copy(alpha = 0.2f), borderWidth = 1.dp)
            ) {
                Text(btnText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
