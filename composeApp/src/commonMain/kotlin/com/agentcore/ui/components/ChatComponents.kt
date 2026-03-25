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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agentcore.api.ApprovalRequestPayload
import kotlinx.serialization.json.JsonObject

@Composable
fun ConnectionStatusBanner(
    statusState: String,
    isDisconnected: Boolean,
    onRestartAgent: () -> Unit
) {
    AnimatedVisibility(
        visible = isDisconnected,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            color = Color(0xFFB71C1C)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (statusState == "CRASHED") "Agent uległ awarii (OOM / SIGKILL)" else "Backend niedostępny — sprawdź połączenie",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
                if (statusState == "CRASHED" || isDisconnected) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onRestartAgent,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB71C1C)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Uruchom ponownie", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    focusRequester: FocusRequester
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text("Szukaj w wiadomościach...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

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
    focusRequester: FocusRequester
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
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
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth().focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                                if (inputText.isNotBlank() || selectedImages.isNotEmpty()) {
                                    onSendMessage(inputText, selectedImages)
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
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || selectedImages.isNotEmpty()) {
                                onSendMessage(inputText, selectedImages)
                            }
                        },
                        enabled = inputText.isNotBlank() || selectedImages.isNotEmpty(),
                        modifier = Modifier.size(32.dp).background(
                            if (inputText.isNotBlank() || selectedImages.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Icon(Icons.Default.Send, null, Modifier.size(16.dp), Color.White)
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isThinking,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(32.dp).background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Icon(Icons.Default.Stop, null, Modifier.size(16.dp), Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingScrollButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp))
        }
    }
}

