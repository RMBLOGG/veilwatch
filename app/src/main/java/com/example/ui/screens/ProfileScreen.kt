package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.UiState
import com.example.ui.VeilwatchViewModel
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: VeilwatchViewModel,
    onNavigateToDetail: (Int) -> Unit
) {
    val userProfileState by viewModel.userProfileState.collectAsState()
    val localWatchlist by viewModel.localWatchlist.collectAsState()
    val aniListToken by viewModel.aniListToken.collectAsState()

    var inputToken by remember { mutableStateOf("") }
    var showAuthDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Text(
            text = "My Profile",
            style = MaterialTheme.typography.displayMedium.copy(color = Color.White),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when (val state = userProfileState) {
            is UiState.Success -> {
                val profile = state.data
                ProfileHeaderCard(
                    profile = profile,
                    onLogout = { viewModel.logoutAniList() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Streaming Statistics",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                StatsGrid(profile = profile)
            }
            else -> {
                // Not authenticated layout prompt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface)
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimaryAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Synchronize Watchlist with AniList",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Authenticate securely to sync your Watching progress list across mobile, tablet, and web clients.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showAuthDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("anilist_login_button")
                        ) {
                            Text("Connect AniList Account")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History list fallback
        Text(
            text = "Local Saved Collection",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (localWatchlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No saved tracking items recorded locally.", color = TextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(localWatchlist.take(9)) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .clickable { onNavigateToDetail(item.animeId) }
                    ) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                        )
                        Text(
                            text = item.animeTitle,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 9.sp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Connect Authentication Dialog modal
        if (showAuthDialog) {
            AlertDialog(
                onDismissRequest = { showAuthDialog = false },
                title = { Text("Authenticate AniList Profile", color = Color.White) },
                text = {
                    Column {
                        Text(
                            text = "To sync, log in on AniList, copy your developer access token from settings, and paste below:",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = inputToken,
                            onValueChange = { inputToken = it },
                            placeholder = { Text("Enter access token string...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = Color.Gray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("token_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputToken.isNotEmpty()) {
                                viewModel.authenticateAniList(inputToken)
                                showAuthDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAuthDialog = false }) {
                        Text("Cancel", color = SecondaryAccent)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
fun ProfileHeaderCard(
    profile: com.example.data.UserProfile,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, Color(0x1BFFFFFF), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.dp, PrimaryAccent, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "AniList Synced Member",
                    style = MaterialTheme.typography.bodyMedium.copy(color = PrimaryAccent)
                )
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red)
            }
        }
    }
}

@Composable
fun StatsGrid(profile: com.example.data.UserProfile) {
    val itemsList = listOf(
        Pair("Total Anime", profile.animeCount.toString()),
        Pair("Mean Score", "%.1f".format(profile.meanScore)),
        Pair("Eps Screened", profile.episodesWatched.toString()),
        Pair("Hours Watched", (profile.minutesWatched / 60).toString())
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsList.forEach { (title, stat) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stat, style = MaterialTheme.typography.titleMedium.copy(color = PrimaryAccent, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
