package com.agentcore.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * Thin wrapper around Material3 PlainTooltip — avoids repeating the
 * ExperimentalMaterial3Api opt-in and the verbose TooltipBox call-site everywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTooltip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState()
    ) {
        content()
    }
}
