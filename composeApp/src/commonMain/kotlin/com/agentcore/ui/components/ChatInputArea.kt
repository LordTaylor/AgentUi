// Chat text-input surface with image attachments, send/cancel buttons, keyboard history navigation,
// slash command autocomplete popup, and drag-and-drop file support.
// See: SlashCommandParser.kt, SlashSuggestionPopup.kt for slash command details.
package com.agentcore.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agentcore.ui.chat.ChatIntent

@Composable
fun ChatInputArea(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    selectedImages: List<String>,
    onRemoveImage: (String) -> Unit,
    onAttachImage: () -> Unit,
    onSendMessage: (String, List<String>) -> Unit,
    onRetryLast: () -> Unit,
    onShowHistory: (String) -> Unit,
    onNavigateHistoryUp: () -> Unit = {},
    onNavigateHistoryDown: () -> Unit = {},
    onCancel: () -> Unit = {},
    isThinking: Boolean = false,
    focusRequester: FocusRequester,
    onSlashCommand: (SlashCommand) -> Unit = {},
    onAddAttachment: (String) -> Unit = {}
) {
    val slashSuggestions = remember(inputText) { SlashCommandParser.suggestions(inputText) }
    val showSlashPopup = slashSuggestions.isNotEmpty() && inputText.startsWith("/")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        // Slash command popup — appears above the input surface
        if (showSlashPopup) {
            SlashSuggestionPopup(
                suggestions = slashSuggestions,
                onSelect = { suggestion ->
                    val completed = suggestion.example.ifEmpty { suggestion.trigger + " " }
                    onInputTextChange(completed)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (selectedImages.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedImages.forEach { imagePath ->
                        Box(modifier = Modifier.size(60.dp)) {
                            AsyncImage(
                                model = imagePath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onRemoveImage(imagePath) },
                                modifier = Modifier.align(Alignment.TopEnd).size(20.dp).offset(x = 6.dp, y = (-6).dp)
                            ) {
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.error) {
                                    Icon(Icons.Default.Close, null, Modifier.size(12.dp), Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.heightIn(max = 150.dp)) {
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                                if (inputText.isNotBlank() || selectedImages.isNotEmpty()) {
                                    val cmd = SlashCommandParser.parse(inputText)
                                    if (cmd != null) { onSlashCommand(cmd); onInputTextChange("") }
                                    else onSendMessage(inputText, selectedImages)
                                }
                                true
                            } else if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                onNavigateHistoryUp()
                                true
                            } else if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                onNavigateHistoryDown()
                                true
                            } else false
                        },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty() && selectedImages.isEmpty()) {
                            Text("Wpisz wiadomość...", color = Color.Gray, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    IconButton(onClick = onAttachImage) {
                        Icon(Icons.Default.Image, "Attach Image", modifier = Modifier.size(20.dp), tint = Color.Gray)
                    }
                    IconButton(onClick = onRetryLast) {
                        Icon(Icons.Default.Refresh, "Retry Last", modifier = Modifier.size(20.dp), tint = Color.Gray)
                    }
                    IconButton(onClick = { onShowHistory(inputText) }) {
                        Icon(Icons.Default.History, "History", modifier = Modifier.size(20.dp), tint = Color.Gray)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // a11y: touch target is 48×48dp; visual pill is 32dp inside via padding
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || selectedImages.isNotEmpty()) {
                                val cmd = SlashCommandParser.parse(inputText)
                                if (cmd != null) { onSlashCommand(cmd); onInputTextChange("") }
                                else onSendMessage(inputText, selectedImages)
                            }
                        },
                        enabled = inputText.isNotBlank() || selectedImages.isNotEmpty(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).background(
                                if (inputText.isNotBlank() || selectedImages.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Wyślij wiadomość",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isThinking,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        // a11y: touch target 48dp, visual pill 32dp
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    RoundedCornerShape(8.dp)
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Zatrzymaj generowanie",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    } // outer Column (wraps popup + Surface)
}
