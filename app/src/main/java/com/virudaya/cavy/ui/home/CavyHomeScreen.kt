package com.virudaya.cavy.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.virudaya.cavy.data.profile.UserProfile
import com.virudaya.cavy.ui.components.CavyBadge
import com.virudaya.cavy.ui.components.CavyCard
import com.virudaya.cavy.ui.components.CavyPrimaryButton
import com.virudaya.cavy.ui.components.CavySecondaryButton
import com.virudaya.cavy.ui.components.CavyTopAppBar
import com.virudaya.cavy.ui.profile.ProfileSection
import com.virudaya.cavy.ui.profile.ProfileUiState
import com.virudaya.cavy.ui.theme.CavyPink
import com.virudaya.cavy.ui.theme.CavyTheme
import kotlinx.coroutines.launch

@Composable
fun CavyHomeScreen(
    profileUiState: ProfileUiState = ProfileUiState(),
    onStartEditDisplayName: () -> Unit = {},
    onDisplayNameChange: (String) -> Unit = {},
    onSaveDisplayName: () -> Unit = {},
    onCancelEditDisplayName: () -> Unit = {},
    onCheckEngineStatus: ((String) -> Unit) -> Unit = {},
    onOpenChats: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CavyTopAppBar() },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Profile Section
            ProfileSection(
                uiState = profileUiState,
                onStartEdit = onStartEditDisplayName,
                onDisplayNameChange = onDisplayNameChange,
                onSaveEdit = onSaveDisplayName,
                onCancelEdit = onCancelEditDisplayName,
                onSignOut = onSignOut
            )

            // Messaging Shortcut Card
            CavyCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenChats
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CavyPink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.size(12.dp))

                        Column {
                            Text(
                                text = "1-on-1 Messages",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Start or continue private Cavy chats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    CavyPrimaryButton(
                        onClick = onOpenChats,
                        text = "Open Chats"
                    )
                }
            }

            // Hero Brand Card
            CavyCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CavyBadge(text = "Phase 2 Secure Messaging")
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(CavyPink, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Cavy Engine",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Native Android application with real-time 1-on-1 messaging, user discovery, and strict security rules.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Interactive Actions Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CavySecondaryButton(
                    onClick = {
                        onCheckEngineStatus { resultMsg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(resultMsg)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    text = "Engine Status"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun CavyHomeScreenLightPreview() {
    CavyTheme(darkTheme = false) {
        CavyHomeScreen(
            profileUiState = ProfileUiState(
                isLoading = false,
                authenticatedUid = "uid1",
                userProfile = UserProfile("uid1", "Jordan Cavy", createdAt = System.currentTimeMillis()),
                email = "jordan@cavy.com"
            )
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CavyHomeScreenDarkPreview() {
    CavyTheme(darkTheme = true) {
        CavyHomeScreen(
            profileUiState = ProfileUiState(
                isLoading = false,
                authenticatedUid = "uid1",
                userProfile = UserProfile("uid1", "Jordan Cavy", createdAt = System.currentTimeMillis()),
                email = "jordan@cavy.com"
            )
        )
    }
}
