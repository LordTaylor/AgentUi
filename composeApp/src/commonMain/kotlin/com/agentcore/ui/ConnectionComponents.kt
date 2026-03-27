package com.agentcore.ui

// Stateless helper composables used by ConnectionScreen:
// Sidebar with navigation, Header with status bar, and ConnectionCard tile.
// Color palette constants for the dark premium theme are also defined here.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Premium Dark Theme Palette
internal val SurfaceDark   = Color(0xFF0D0E14)
internal val SidebarDark   = Color(0xFF14151B)
internal val CardDark      = Color(0xFF1A1B22)
internal val AccentPurple  = Color(0xFFB283FF)
internal val AccentTeal    = Color(0xFF4FD1C5)
internal val TextSecondary = Color(0xFF94959B)

@Composable
internal fun ConnectionSidebar(modifier: Modifier = Modifier) {
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
internal fun ConnectionHeader(isSmallScreen: Boolean) {
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
internal fun ConnectionCard(
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
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = Color.White.copy(alpha = 0.05f),
            borderWidth = 1.dp
        )
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
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = Color.White.copy(alpha = 0.2f),
                    borderWidth = 1.dp
                )
            ) {
                Text(btnText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
