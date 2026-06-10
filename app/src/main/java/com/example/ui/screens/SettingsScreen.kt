package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VeilwatchViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: VeilwatchViewModel,
    onNavigateBack: () -> Unit
) {
    val titleLang by viewModel.titleLanguage.collectAsState()
    val preferredServer by viewModel.preferredServer.collectAsState()
    val preferredCategory by viewModel.preferredCategory.collectAsState()
    val adultContentEnabled by viewModel.adultContentEnabled.collectAsState()

    var showResetConfirmed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // Settings Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(DarkSurface)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Preferences Settings",
                style = MaterialTheme.typography.displayMedium.copy(color = Color.White)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            // Title Lang Picker Card
            item {
                SettingsGroupHeader(title = "TYPOGRAPHY VISUALS")
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Title Language Format", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Choose the default writing format for anime titles", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("romaji" to "Romaji", "english" to "English", "native" to "Native").forEach { (key, label) ->
                                val selected = titleLang == key
                                Button(
                                    onClick = { viewModel.updateTitleLang(key) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) PrimaryAccent else DarkCard
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("title_lang_$key")
                                ) {
                                    Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Stream settings
            item {
                SettingsGroupHeader(title = "STREAM PLAYER OPTIONS")
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Preferred Server Track", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Select high-speed scraper helper media nodes", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("HD-1", "HD-2").forEach { svr ->
                                val selected = preferredServer == svr
                                Button(
                                    onClick = { viewModel.changeStreamingServer(svr) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) PrimaryAccent else DarkCard
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(svr, color = if (selected) Color.White else TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Adult toggle
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Filter Adult Materials", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Exclude 18+ items dynamically from general recommendations and search pages", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = adultContentEnabled,
                            onCheckedChange = { viewModel.updateAdultContent(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryAccent,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = DarkCard
                            ),
                            modifier = Modifier.testTag("adult_toggle")
                        )
                    }
                }
            }

            // Local cache wipe settings
            item {
                SettingsGroupHeader(title = "DEVICE MAINTENANCE")
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Clear Local History Store", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Wipes localized continues and metrics history safely off disk", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.deleteHistory()
                                showResetConfirmed = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clear_history_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Flush Statistics", color = Color.White)
                        }
                    }
                }
            }
        }

        if (showResetConfirmed) {
            AlertDialog(
                onDismissRequest = { showResetConfirmed = false },
                title = { Text("Metrics Cleared", color = Color.White) },
                text = { Text("Device history database wiped successfully. Streaming indexes are optimized.", color = TextSecondary) },
                confirmButton = {
                    Button(onClick = { showResetConfirmed = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)) {
                        Text("Confirm")
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(color = PrimaryAccent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
        content = { content() }
    )
}
