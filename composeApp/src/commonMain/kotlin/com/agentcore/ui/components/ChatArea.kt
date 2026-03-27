// Main chat message list with auto-scroll, cauldron background, loading overlay, and search/scroll overlays.
// Auto-scroll logic tracks bottom position and re-enables when user scrolls back down.
// See: ChatBubble.kt for individual message rendering; ChatComponents.kt for overlay helpers.
package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shadow
import com.agentcore.model.Message
import com.agentcore.ui.chat.ChatIntent
import com.agentcore.ui.components.cauldron.CauldronState
import com.agentcore.ui.components.cauldron.WitchCauldron
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.agentcore.shared.ConnectionMode

@Composable
fun ChatArea(
    messages: List<Message>,
    filteredMessages: List<Message>,
    statusState: String,
    cauldronState: CauldronState,
    cauldronGridSize: Int,
    listState: LazyListState,
    showSearch: Boolean,
    messageSearchQuery: String,
    onFork: (Int) -> Unit,
    onSendMessage: (String, List<String>) -> Unit,
    onIntent: (ChatIntent, CoroutineScope, com.agentcore.shared.ConnectionMode) -> Unit,
    scope: CoroutineScope,
    mode: com.agentcore.shared.ConnectionMode,
    chatFontSize: Float,
    codeFontSize: Float,
    showScrollToBottom: Boolean,
    searchFocusRequester: androidx.compose.ui.focus.FocusRequester,
    loadingModelName: String? = null,
    modifier: Modifier = Modifier
) {
    var autoScroll by remember { mutableStateOf(true) }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) true
            else {
                val lastVisibleItem = visibleItems.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
        }
    }

    // Auto-disable when user scrolls up; re-enable when they reach the bottom
    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress to isAtBottom }
            .collect { (scrolling, atBottom) ->
                if (scrolling && !atBottom) autoScroll = false
                if (atBottom) autoScroll = true
            }
    }

    // Re-trigger on every TextDelta (lastMsgLen changes) AND on new messages
    val lastMsgLen = filteredMessages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(filteredMessages.size, lastMsgLen, statusState) {
        if (autoScroll) {
            val lastIdx = filteredMessages.size + (if (statusState == "THINKING") 1 else 0) - 1
            if (lastIdx >= 0) listState.animateScrollToItem(lastIdx)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        WitchCauldron(
            state = cauldronState,
            modifier = Modifier.size(500.dp).align(Alignment.Center).alpha(0.15f),
            gridSize = cauldronGridSize
        )

        if (cauldronState == CauldronState.LOADING) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(top = 180.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Summoning ${loadingModelName ?: "Model"}...",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFFE1BEE7),
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(color = Color.Black, blurRadius = 10f)
                    )
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredMessages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (messageSearchQuery.isEmpty()) Icons.Default.MailOutline else Icons.Default.SearchOff,
                                    null,
                                    Modifier.size(64.dp),
                                    Color.Gray.copy(alpha = 0.3f)
                                )
                                Text(
                                    if (messageSearchQuery.isEmpty()) "Brak wiadomości" else "Nie znaleziono wiadomości",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(filteredMessages, key = { _, msg -> msg.id }) { index, msg ->
                        val isGrouped = index > 0 && run {
                            val prev = filteredMessages[index - 1]
                            val timeDiff = msg.timestamp - prev.timestamp
                            prev.sender == msg.sender && timeDiff < 300000 // 5 minutes in ms
                        }
                        val isLastAgentMsg = !msg.isFromUser &&
                            index == filteredMessages.lastIndex &&
                            statusState == "THINKING"
                        ChatBubble(
                            msg = msg,
                            isGrouped = isGrouped,
                            isStreaming = isLastAgentMsg,
                            fontSize = chatFontSize,
                            codeFontSize = codeFontSize,
                            onFork = { onFork(index) },
                            onRetry = { onSendMessage(msg.text, emptyList()) },
                            onEdit = { text -> onIntent(ChatIntent.PasteToInput(text), scope, mode) },
                            onRunCode = { code -> onIntent(ChatIntent.PasteToInput(code), scope, mode) },
                            onRunInTerminal = { code ->
                                onSendMessage("/bash $code", emptyList())
                            }
                        )
                    }
                    if (statusState == "THINKING") {
                        item {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(start = 24.dp))
                        }
                    }
                }
            }
        }

        // Auto-scroll toggle buttons — top-left corner
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activeColor = MaterialTheme.colorScheme.primary
            val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            IconButton(
                onClick = { autoScroll = false },
                modifier = Modifier.size(28.dp)
                    .background(
                        if (!autoScroll) activeColor.copy(alpha = 0.18f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(Icons.Default.Pause, "Stop auto-scroll", Modifier.size(14.dp),
                    tint = if (!autoScroll) activeColor else inactiveColor)
            }
            IconButton(
                onClick = {
                    autoScroll = true
                    scope.launch { listState.animateScrollToItem(messages.size) }
                },
                modifier = Modifier.size(28.dp)
                    .background(
                        if (autoScroll) activeColor.copy(alpha = 0.18f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(Icons.Default.PlayArrow, "Start auto-scroll", Modifier.size(14.dp),
                    tint = if (autoScroll) activeColor else inactiveColor)
            }
        }

        // Search field overlay
        if (showSearch) {
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                MessageSearchField(
                    query = messageSearchQuery,
                    onQueryChange = { onIntent(ChatIntent.UpdateSearchQuery(it), scope, mode) },
                    onDismiss = { onIntent(ChatIntent.ToggleSearch, scope, mode) },
                    focusRequester = searchFocusRequester
                )
            }
        }

        // Floating scroll button
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            FloatingScrollButton(
                visible = showScrollToBottom && messageSearchQuery.isEmpty(),
                onClick = {
                    scope.launch { listState.animateScrollToItem(messages.size) }
                }
            )
        }
    }
}
