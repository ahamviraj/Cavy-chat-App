package com.virudaya.cavy.ui.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.virudaya.cavy.data.profile.UserProfile
import com.virudaya.cavy.ui.components.CavyBadge
import com.virudaya.cavy.ui.components.CavyCard
import com.virudaya.cavy.ui.components.CavySecondaryButton
import com.virudaya.cavy.ui.components.CavyTextField
import com.virudaya.cavy.ui.theme.CavyPink
import com.virudaya.cavy.ui.theme.CavyTheme
import com.virudaya.cavy.ui.theme.CavyWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileSection(
    uiState: ProfileUiState,
    onStartEdit: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    CavyCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val initial = uiState.userProfile?.displayName?.firstOrNull()?.uppercaseChar()
                        ?: uiState.email.firstOrNull()?.uppercaseChar()
                        ?: 'C'
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CavyPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CavyWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Account Profile",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val uidDisplay = uiState.authenticatedUid?.take(8)
                            ?: uiState.userProfile?.uid?.take(8)
                            ?: "Unauthenticated"
                        CavyBadge(text = "UID: $uidDisplay")
                    }
                }

                CavySecondaryButton(
                    onClick = onSignOut,
                    text = "Sign Out"
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CavyPink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (uiState.successMessage != null) {
                Text(
                    text = uiState.successMessage,
                    color = CavyPink,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Display Name Section
            if (uiState.isEditingDisplayName) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CavyTextField(
                        value = uiState.editDisplayNameInput,
                        onValueChange = onDisplayNameChange,
                        label = "Display Name",
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onSaveEdit) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Save display name",
                            tint = CavyPink
                        )
                    }

                    IconButton(onClick = onCancelEdit) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel editing"
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Display Name",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.userProfile?.displayName ?: "Cavy Member",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onStartEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit display name"
                        )
                    }
                }
            }

            // Private Email
            Column {
                Text(
                    text = "Private Account Email",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.email.ifBlank { "Unregistered" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Joined Date
            if (uiState.userProfile != null && uiState.userProfile.createdAt > 0L) {
                val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(uiState.userProfile.createdAt))
                Text(
                    text = "Joined $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Light Mode Profile", showBackground = true)
@Composable
fun ProfileSectionLightPreview() {
    CavyTheme(darkTheme = false) {
        ProfileSection(
            uiState = ProfileUiState(
                isLoading = false,
                authenticatedUid = "uid12345678",
                userProfile = UserProfile("uid12345678", "Alex Rivers", createdAt = System.currentTimeMillis()),
                email = "alex@cavy.com"
            ),
            onStartEdit = {},
            onDisplayNameChange = {},
            onSaveEdit = {},
            onCancelEdit = {},
            onSignOut = {}
        )
    }
}

@Preview(name = "Dark Mode Profile", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileSectionDarkPreview() {
    CavyTheme(darkTheme = true) {
        ProfileSection(
            uiState = ProfileUiState(
                isLoading = false,
                authenticatedUid = "uid12345678",
                userProfile = UserProfile("uid12345678", "Alex Rivers", createdAt = System.currentTimeMillis()),
                email = "alex@cavy.com"
            ),
            onStartEdit = {},
            onDisplayNameChange = {},
            onSaveEdit = {},
            onCancelEdit = {},
            onSignOut = {}
        )
    }
}
