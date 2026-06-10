package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.UiState
import com.example.ui.VeilwatchViewModel
import com.example.ui.theme.*

@Composable
fun DetailScreen(
    animeId: Int,
    viewModel: VeilwatchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Int, Int) -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    val localWatchlist by viewModel.localWatchlist.collectAsState()
    val activeEpisodes by viewModel.activeEpisodes.collectAsState()
    val titleLang by viewModel.titleLanguage.collectAsState()

    var activeTab by remember { mutableStateOf("EPISODES") } // "EPISODES", "CHARACTERS", "RELATIONS", "RECOMMENDATIONS"
    var showTrackSheet by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = animeId) {
        viewModel.loadAnimeDetail(animeId)
    }

    val localItem = localWatchlist.firstOrNull { it.id == animeId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        when (val state = detailState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            }
            is UiState.Success -> {
                val anime = state.data.media
                val isFavorite = localItem != null // fallback toggle favoring

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Header Banner & Cover
                    item {
                        DetailHeaderSection(
                            anime = anime,
                            onBack = onNavigateBack,
                            onToggleFav = { viewModel.toggleFavorite(anime.id) }
                        )
                    }

                    // Metadata Row
                    item {
                        DetailMetaChips(anime = anime)
                    }

                    // Action buttons (Play & Track)
                    item {
                        ActionButtonsRow(
                            hasEpisodes = activeEpisodes.isNotEmpty(),
                            localStatus = localItem?.status ?: "ADD TO WATCHLIST",
                            onPlayFirst = {
                                if (activeEpisodes.isNotEmpty()) {
                                    val firstEp = activeEpisodes.first()
                                    viewModel.selectEpisodeToPlay(firstEp)
                                    onNavigateToPlayer(anime.id, firstEp.number)
                                }
                            },
                            onTrackClick = { showTrackSheet = true }
                        )
                    }

                    // Expandable Synopsis Description
                    item {
                        ExpandableDescription(description = anime.description ?: "No description available.")
                    }

                    // Tabs picker
                    item {
                        DetailTabsSelector(
                            activeTab = activeTab,
                            onTabSelected = { activeTab = it }
                        )
                    }

                    // Tab contents
                    when (activeTab) {
                        "EPISODES" -> {
                            if (activeEpisodes.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "Loading streaming index... Standard players will load briefly.", color = TextSecondary)
                                    }
                                }
                            } else {
                                items(activeEpisodes) { ep ->
                                    EpisodeRowItem(
                                        episode = ep,
                                        onEpisodeClick = {
                                            viewModel.selectEpisodeToPlay(ep)
                                            onNavigateToPlayer(anime.id, ep.number)
                                        }
                                    )
                                }
                            }
                        }
                        "CHARACTERS" -> {
                            if (state.data.characters.isEmpty()) {
                                item {
                                    Text(
                                        text = "No characters details found in AniList.",
                                        color = TextSecondary,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            } else {
                                item {
                                    CharactersGridRow(characters = state.data.characters)
                                }
                            }
                        }
                        "RELATIONS" -> {
                            if (state.data.relations.isEmpty()) {
                                item {
                                    Text(
                                        text = "No spin-offs or prequel releases associated.",
                                        color = TextSecondary,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            } else {
                                items(state.data.relations) { relation ->
                                    RelationRowItem(
                                        relation = relation,
                                        onClick = { viewModel.loadAnimeDetail(relation.id) }
                                    )
                                }
                            }
                        }
                        "RECOMMENDATIONS" -> {
                            if (state.data.recommendations.isEmpty()) {
                                item {
                                    Text(
                                        text = "Suggestions pool is forming. Check back details later.",
                                        color = TextSecondary,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            } else {
                                item {
                                    RecommendationsSection(
                                        recommendations = state.data.recommendations,
                                        titleLang = titleLang,
                                        onAnimeClick = { viewModel.loadAnimeDetail(it.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Tracking updates overlay drawer bottom sheet
                if (showTrackSheet) {
                    TrackingBottomDrawer(
                        anime = anime,
                        currentItem = localItem,
                        onDismiss = { showTrackSheet = false },
                        onSave = { status, score, progress ->
                            viewModel.updateWatchlist(anime, status, score, progress)
                            showTrackSheet = false
                        }
                    )
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = SecondaryAccent)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)) {
                            Text("Back Home")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailHeaderSection(
    anime: AnimeMedia,
    onBack: () -> Unit,
    onToggleFav: () -> Unit
) {
    val bannerUrl = anime.bannerImage ?: anime.coverImage

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        // Banner Backdrop
        AsyncImage(
            model = bannerUrl,
            contentDescription = "Detail Banner",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentScale = ContentScale.Crop
        )

        // Banner gradient vignette
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, DarkBg),
                        startY = 100f
                    )
                )
        )

        // Navigation Header Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            IconButton(
                onClick = onToggleFav,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = SecondaryAccent)
            }
        }

        // Foreground Poster and Cover titles overlap
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            AsyncImage(
                model = anime.coverImage,
                contentDescription = "Cover",
                modifier = Modifier
                    .size(width = 100.dp, height = 145.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(
                    text = anime.titleRomaji ?: anime.titleEnglish ?: "No title",
                    style = MaterialTheme.typography.displayMedium.copy(color = Color.White),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anime.studioName ?: "Airing/Production unknown studio",
                    color = PrimaryAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun DetailMetaChips(anime: AnimeMedia) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        anime.format?.let { fmt ->
            AssistChip(
                onClick = {},
                label = { Text(fmt) },
                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White, containerColor = DarkCard)
            )
        }
        anime.episodes?.let { eps ->
            AssistChip(
                onClick = {},
                label = { Text("$eps Episodes") },
                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White, containerColor = DarkCard)
            )
        }
        anime.averageScore?.let { score ->
            AssistChip(
                onClick = {},
                label = { Text("Score $score%") },
                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White, containerColor = DarkCard)
            )
        }
        anime.status?.let { status ->
            AssistChip(
                onClick = {},
                label = { Text(status) },
                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White, containerColor = DarkCard)
            )
        }
        anime.season?.let { season ->
            AssistChip(
                onClick = {},
                label = { Text("$season ${anime.seasonYear ?: ""}") },
                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White, containerColor = DarkCard)
            )
        }
    }
}

@Composable
fun ActionButtonsRow(
    hasEpisodes: Boolean,
    localStatus: String,
    onPlayFirst: () -> Unit,
    onTrackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPlayFirst,
            enabled = hasEpisodes,
            modifier = Modifier.weight(1f).height(50.dp).testTag("play_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAccent,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (hasEpisodes) "Play Episode 1" else "Streaming Unvailable", color = Color.White)
        }

        OutlinedButton(
            onClick = onTrackClick,
            modifier = Modifier.weight(1f).height(50.dp).testTag("track_button"),
            border = BorderStroke(1.dp, PrimaryAccent),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent)
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(localStatus.replace("_", " "), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ExpandableDescription(description: String) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Strip HTML tags if any from description
        val stripped = description.replace(Regex("<[^>]*>"), "")

        Text(
            text = stripped,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            maxLines = if (expanded) 100 else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.animateContentSize()
        )

        Text(
            text = if (expanded) "Show Less" else "Show More",
            color = PrimaryAccent,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
fun DetailTabsSelector(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("EPISODES", "CHARACTERS", "RELATIONS", "RECOMMENDATIONS")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val isActive = activeTab == tab
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (isActive) Color.White else TextSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 3.dp)
                                .clip(CircleShape)
                                .background(PrimaryAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeRowItem(
    episode: HiAnimeEpisodeDto,
    onEpisodeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable { onEpisodeClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = episode.number.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(color = PrimaryAccent, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title ?: "Episode ${episode.number}",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "High speed streaming direct server",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = PrimaryAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun CharactersGridRow(characters: List<AnimeCharacter>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Major Characters",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(characters) { char ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(85.dp)
                ) {
                    AsyncImage(
                        model = char.imageUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(1.dp, PrimaryAccent.copy(alpha = 0.3f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = char.name,
                        style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = char.role.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun RelationRowItem(
    relation: AnimeRelation,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = relation.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 50.dp, height = 75.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = relation.relationType.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall.copy(color = PrimaryAccent, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = relation.title,
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${relation.format ?: ""} • ${relation.status ?: ""}",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun RecommendationsSection(
    recommendations: List<AnimeMedia>,
    titleLang: String,
    onAnimeClick: (AnimeMedia) -> Unit
) {
    Column {
        Text(
            text = "People Also Watch",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
            modifier = Modifier.padding(16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(recommendations) { anime ->
                AnimeCard(
                    anime = anime,
                    titleLang = titleLang,
                    onClick = { onAnimeClick(anime) }
                )
            }
        }
    }
}

@Composable
fun TrackingBottomDrawer(
    anime: AnimeMedia,
    currentItem: WatchlistItem?,
    onDismiss: () -> Unit,
    onSave: (status: String, score: Double, progress: Int) -> Unit
) {
    var statusState by remember { mutableStateOf(currentItem?.status ?: "CURRENT") }
    var scoreState by remember { mutableStateOf(currentItem?.score ?: 8.0) }
    var progressState by remember { mutableStateOf(currentItem?.progress ?: 0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        // Sheet content
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DarkSurface)
                .clickable(enabled = false) {} // non-dismiss click interceptor
                .padding(24.dp)
        ) {
            Column {
                // Drag handle element
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Track Watchlist Settings",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                )
                Text(
                    text = anime.titleRomaji ?: anime.titleEnglish ?: "Anime Title",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Watch Status tabs Selection List
                Text(text = "Watching Status", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                val statuses = listOf("CURRENT", "COMPLETED", "PLANNING", "ON_HOLD", "DROPPED")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(statuses) { stat ->
                        val selected = statusState == stat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) PrimaryAccent else DarkCard)
                                .clickable { statusState = stat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stat.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (selected) Color.White else TextSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Score Selector
                Text(
                    text = "Personal Score: ${"%.1f".format(scoreState)} / 10.0",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = scoreState.toFloat(),
                    onValueChange = { scoreState = it.toDouble() },
                    valueRange = 0f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAccent,
                        activeTrackColor = PrimaryAccent,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Progress picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Episodes Watched", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "Airing: ${anime.episodes ?: "No limit"}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (progressState > 0) progressState-- },
                            modifier = Modifier.background(DarkCard, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                        }
                        Text(
                            text = progressState.toString(),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(
                            onClick = {
                                val limit = anime.episodes ?: 999
                                if (progressState < limit) progressState++
                            },
                            modifier = Modifier.background(DarkCard, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save buttons CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onSave(statusState, scoreState, progressState) },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("save_track_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Tracker")
                    }
                }
            }
        }
    }
}
