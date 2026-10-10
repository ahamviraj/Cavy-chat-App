package com.virudaya.cavy.ui.messaging

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.virudaya.cavy.data.messaging.ConversationSummary
import com.virudaya.cavy.data.profile.UserProfile
import com.virudaya.cavy.ui.components.CavyCard
import com.virudaya.cavy.ui.components.CavyTopAppBar
import com.virudaya.cavy.ui.theme.CavyPink
import com.virudaya.cavy.ui.theme.CavyTheme
import com.virudaya.cavy.ui.theme.CavyWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationListScreen(
    uiState: ConversationListUiState,
    onOpenUserDiscovery: () -> Unit,
    onCloseUserDiscovery: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onUserSelected: (UserProfile) -> Unit,
    onConversationClicked: (conversationId: String, otherUid: String, otherDisplayName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CavyTopAppBar(title = "Cavy Chats") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenUserDiscovery,
                containerColor = CavyPink,
                contentColor = CavyWhite,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Conversation"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.errorMessage != null) {
                CavyCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CavyPink)
                }
            } else if (uiState.conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No active conversations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to start a private chat with a Cavy member.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.conversations) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = {
                                onConversationClicked(
                                    conv.conversationId,
                                    conv.otherUid,
                                    conv.otherDisplayName
                                )
                            }
                        )
                    }
                }
            }
        }

        if (uiState.isSearchingUsers) {
            UserDiscoveryDialog(
                searchQuery = uiState.userSearchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                users = uiState.availableUsers,
                isLoading = uiState.isSearchingLoading,
                onUserSelected = onUserSelected,
                onDismiss = onCloseUserDiscovery
            )
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CavyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = conversation.otherDisplayName.firstOrNull()?.uppercaseChar() ?: 'C'
            Box(
                modifier = Modifier
                    .size(48.dp)
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

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.otherDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (conversation.lastMessageTimestamp > 0L) {
                        val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault())
                            .format(Date(conversation.lastMessageTimestamp))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = conversation.lastMessageText.ifBlank { "Tap to start messaging" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun ConversationListLightPreview() {
    CavyTheme(darkTheme = false) {
        ConversationListScreen(
            uiState = ConversationListUiState(
                isLoading = false,
                conversations = listOf(
                    ConversationSummary(
                        conversationId = "1",
                        otherUid = "u2",
                        otherDisplayName = "Viraj Cavy",
                        lastMessageText = "Hey! How is Phase 2 going?",
                        lastMessageTimestamp = System.currentTimeMillis()
                    )
                )
            ),
            onOpenUserDiscovery = {},
            onCloseUserDiscovery = {},
            onSearchQueryChanged = {},
            onUserSelected = {},
            onConversationClicked = { _, _, _ -> }
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ConversationListDarkPreview() {
    CavyTheme(darkTheme = true) {
        ConversationListScreen(
            uiState = ConversationListUiState(
                isLoading = false,
                conversations = listOf(
                    ConversationSummary(
                        conversationId = "1",
                        otherUid = "u2",
                        otherDisplayName = "Viraj Cavy",
                        lastMessageText = "Hey! How is Phase 2 going?",
                        lastMessageTimestamp = System.currentTimeMillis()
                    )
                )
            ),
            onOpenUserDiscovery = {},
            onCloseUserDiscovery = {},
            onSearchQueryChanged = {},
            onUserSelected = {},
            onConversationClicked = { _, _, _ -> }
        )
    }
}
