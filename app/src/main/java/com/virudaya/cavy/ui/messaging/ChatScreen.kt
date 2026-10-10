package com.virudaya.cavy.ui.messaging

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virudaya.cavy.data.messaging.ChatMessage
import com.virudaya.cavy.ui.theme.CavyPink
import com.virudaya.cavy.ui.theme.CavyTheme
import com.virudaya.cavy.ui.theme.CavyWhite
import com.virudaya.cavy.util.EmojiUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    currentUid: String,
    uiState: ChatUiState,
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var textState by remember { mutableStateOf(TextFieldValue(uiState.messageInput)) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.isImeVisible

    // Sync input when draft is cleared after successful send
    LaunchedEffect(uiState.messageInput) {
        if (uiState.messageInput.isEmpty() && textState.text.isNotEmpty()) {
            textState = TextFieldValue("")
        }
    }

    // Scroll to latest message on new message or keyboard open
    LaunchedEffect(uiState.messages.size, isImeVisible) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val initial = uiState.otherDisplayName.firstOrNull()?.uppercaseChar() ?: 'C'
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CavyPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CavyWhite
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = uiState.otherDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "UID: ${uiState.otherUid.take(8)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Messages Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (uiState.isLoading && uiState.messages.isEmpty()) {
                    CircularProgressIndicator(
                        color = CavyPink,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                } else if (uiState.messages.isEmpty()) {
                    Text(
                        text = "No messages yet. Send a message to start chatting!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        items(uiState.messages) { message ->
                            MessageBubble(
                                message = message,
                                isFromCurrentUser = message.senderUid == currentUid
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }

            // Bottom Composer Row: [ 📎 ] [ Input field ] [ 😊 ] [ ➤ ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment Button (Placeholder)
                IconButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Attachments feature coming soon!")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attachment Placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Input Text Field
                OutlinedTextField(
                    value = textState,
                    onValueChange = { newTextValue ->
                        textState = newTextValue
                        onMessageInputChanged(newTextValue.text)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(text = "Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            keyboardController?.hide()
                            showEmojiPicker = false
                            onSendMessage()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CavyPink,
                        focusedLabelColor = CavyPink,
                        cursorColor = CavyPink
                    ),
                    enabled = !uiState.isSending
                )

                // Emoji Picker Toggle Button
                IconButton(
                    onClick = {
                        showEmojiPicker = !showEmojiPicker
                        if (showEmojiPicker) {
                            keyboardController?.hide()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEmotions,
                        contentDescription = "Emoji Picker",
                        tint = if (showEmojiPicker) CavyPink else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        showEmojiPicker = false
                        onSendMessage()
                    },
                    enabled = textState.text.isNotBlank() && !uiState.isSending
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            color = CavyPink,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (textState.text.isNotBlank()) CavyPink else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Emoji Selection Panel
            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(EmojiUtils.COMMON_EMOJIS) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val updatedValue = EmojiUtils.insertEmojiAtCursor(textState, emoji)
                                        textState = updatedValue
                                        onMessageInputChanged(updatedValue.text)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val bubbleShape = if (isFromCurrentUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val backgroundColor = if (isFromCurrentUser) CavyPink else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isFromCurrentUser) CavyWhite else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(backgroundColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }

        if (message.sentAt > 0L) {
            val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.sentAt))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun ChatScreenLightPreview() {
    CavyTheme(darkTheme = false) {
        ChatScreen(
            currentUid = "u1",
            uiState = ChatUiState(
                otherUid = "u2",
                otherDisplayName = "Viraj Cavy",
                messages = listOf(
                    ChatMessage("1", "c1", "u2", "Hey Jordan!", System.currentTimeMillis() - 60000),
                    ChatMessage("2", "c1", "u1", "Hey Viraj, Phase 2 is running natively! 😀", System.currentTimeMillis())
                )
            ),
            onMessageInputChanged = {},
            onSendMessage = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChatScreenDarkPreview() {
    CavyTheme(darkTheme = true) {
        ChatScreen(
            currentUid = "u1",
            uiState = ChatUiState(
                otherUid = "u2",
                otherDisplayName = "Viraj Cavy",
                messages = listOf(
                    ChatMessage("1", "c1", "u2", "Hey Jordan!", System.currentTimeMillis() - 60000),
                    ChatMessage("2", "c1", "u1", "Hey Viraj, Phase 2 is running natively! 😀", System.currentTimeMillis())
                )
            ),
            onMessageInputChanged = {},
            onSendMessage = {},
            onNavigateBack = {}
        )
    }
}
