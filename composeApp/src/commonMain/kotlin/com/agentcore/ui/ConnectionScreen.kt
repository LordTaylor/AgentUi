package com.agentcore.ui

// Entry-point screen for choosing a backend connection mode.
// Renders a responsive layout: optional sidebar, hero heading, and a grid of ConnectionCards.
// Helper composables (ConnectionSidebar, ConnectionHeader, ConnectionCard) live in ConnectionComponents.kt.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            // --- Sidebar (hidden on small screens) ---
            if (!isSmallScreen) {
                ConnectionSidebar(modifier = Modifier.width(280.dp).fillMaxHeight())
            }

            // --- Main Content ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
            ) {
                ConnectionHeader(isSmallScreen = isSmallScreen)

                Spacer(modifier = Modifier.height(64.dp))

                HeroSection(isSmallScreen = isSmallScreen)

                Spacer(modifier = Modifier.height(64.dp))

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
private fun HeroSection(isSmallScreen: Boolean) {
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
            style = if (isSmallScreen) MaterialTheme.typography.headlineLarge
                    else MaterialTheme.typography.displayMedium,
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
}

@Composable
private fun ConnectionCardsGrid(
    columns: Int,
    onConnect: (ConnectionMode) -> Unit
) {
    val cards = listOf(
        Triple(
            "IPC Server",
            "Connect via background server using HTTP or SSE protocols. Ideal for web-integrated workflows.",
            ConnectionMode.IPC to Icons.Default.Settings
        ),
        Triple(
            "Unix Socket",
            "Native high-speed transport via .sock file. Minimum latency for local system operations.",
            ConnectionMode.UNIX_SOCKET to Icons.Default.Build
        ),
        Triple(
            "STDIO Mode",
            "Persistent local link via process pipes. Reliable, secure, and compatible with most IDEs.",
            ConnectionMode.STDIO to Icons.Default.Check
        ),
        Triple(
            "CLI Direct",
            "One-shot execution of the agent binary. Best for automated scripts and quick queries.",
            ConnectionMode.CLI to Icons.Default.PlayArrow
        )
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
                            btnText = when (modeIcon.first) {
                                ConnectionMode.IPC         -> "INITIALIZE TRANSPORT"
                                ConnectionMode.UNIX_SOCKET -> "MOUNT SOCKET"
                                ConnectionMode.STDIO       -> "ESTABLISH PIPE"
                                else                       -> "EXECUTE BINARY"
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
