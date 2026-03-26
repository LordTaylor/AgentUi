// @Preview composables for WitchCauldron — visible only in IDE preview, not shipped.
package com.agentcore.ui.components.cauldron

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun WitchCauldronPreview() {
    val c = WitchCauldronConstants
    MaterialTheme {
        Surface(color = Color(0xFF121212)) {
            Column(modifier = Modifier.padding(c.PREVIEW_PADDING_DP.dp)) {
                Row {
                    CauldronPreviewItem("IDLE", CauldronState.IDLE)
                    CauldronPreviewItem("THINKING", CauldronState.THINKING)
                }
                Row {
                    CauldronPreviewItem("SENDING", CauldronState.SENDING)
                    CauldronPreviewItem("RECEIVING", CauldronState.RECEIVING)
                }
                Row {
                    CauldronPreviewItem("LOADING", CauldronState.LOADING)
                }
            }
        }
    }
}

@Composable
private fun CauldronPreviewItem(label: String, state: CauldronState) {
    val c = WitchCauldronConstants
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(c.PREVIEW_ITEM_PADDING_DP.dp)
    ) {
        Text(label, color = Color.White, fontSize = c.PREVIEW_LABEL_FONT_SIZE_SP.sp)
        WitchCauldron(state = state, gridSize = 64, modifier = Modifier.size(c.PREVIEW_ITEM_SIZE_DP.dp))
    }
}
