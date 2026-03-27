// Preview composable for ChatArea, isolated to keep ChatArea.kt under the line limit.
// Used exclusively by Android Studio / IDEA Compose preview tooling.
// See: ChatArea.kt for the actual implementation.
package com.agentcore.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.agentcore.ui.components.cauldron.CauldronState
import com.agentcore.shared.ConnectionMode

@Preview
@Composable
fun ChatAreaPreview() {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val searchFocusRequester = androidx.compose.ui.focus.FocusRequester()
    MaterialTheme {
        ChatArea(
            messages = emptyList(),
            filteredMessages = emptyList(),
            statusState = "IDLE",
            cauldronState = CauldronState.IDLE,
            cauldronGridSize = 32,
            listState = rememberLazyListState(),
            showSearch = false,
            messageSearchQuery = "",
            onFork = {},
            onSendMessage = { _, _ -> },
            onIntent = { _, _, _ -> },
            scope = scope,
            mode = ConnectionMode.STDIO,
            chatFontSize = 14f,
            codeFontSize = 13f,
            showScrollToBottom = true,
            searchFocusRequester = searchFocusRequester
        )
    }
}
