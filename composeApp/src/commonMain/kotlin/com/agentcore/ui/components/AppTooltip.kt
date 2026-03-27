package com.agentcore.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

/**
 * Thin wrapper around Material3 PlainTooltip — avoids repeating the
 * ExperimentalMaterial3Api opt-in and the verbose TooltipBox call-site everywhere.
 *
 * a11y: tooltip text is forced to 12sp minimum. PlainTooltip inherits
 * MaterialTheme.typography.labelSmall which resolves to 11sp on desktop —
 * below the recommended 12sp minimum for auxiliary labels (Material 3 / WCAG 1.4.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTooltip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(text, fontSize = 12.sp)
            }
        },
        state = rememberTooltipState()
    ) {
        content()
    }
}
