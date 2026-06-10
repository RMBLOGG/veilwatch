package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AnimeMedia
import com.example.data.WatchHistory
import com.example.ui.UiState
import com.example.ui.VeilwatchViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: VeilwatchViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToWatch: (Int, Int) -> Unit
) {
    val trendingState by viewModel.trendingState.collectAsState()
    val popularState by viewModel.popularState.collectAsState()
    val titleLang by viewModel.titleLanguage.collectAsState()
    val history by viewModel.watchHistory.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Spotlight Carousel
        item {
            when (val state = trendingState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                }
                is UiState.Success -> {
                    SpotlightCarousel(
                        animes = state.data,
                        titleLang = titleLang,
                        onAnimeSelected = { onNavigateToDetail(it.id) }
                    )
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = SecondaryAccent)
                    }
                }
            }
        }

        // Action/Discover banner bar (ShonenX style)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            DiscoverPromoCard(onClick = onNavigateToDiscover)
        }

        // Continue watching history row
        if (history.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HistorySection(
                    historyList = history,
                    onHistorySelected = { animeId, ep -> onNavigateToWatch(animeId, ep) }
                )
            }
        }

        // Popular Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Popular Hits",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (val state = popularState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                }
                is UiState.Success -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.data) { anime ->
                            AnimeCard(
                                anime = anime,
                                titleLang = titleLang,
                                onClick = { onNavigateToDetail(anime.id) }
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = "Error: " + state.message,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // Seasonal Highlights / Trending
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Trending Releases",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (val state = trendingState) {
                is UiState.Success -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.data.reversed()) { anime ->
                            AnimeCard(
                                anime = anime,
                                titleLang = titleLang,
                                onClick = { onNavigateToDetail(anime.id) }
                            )
                        }
                    }
                }
                else -> { /* handled by loading spinners above */ }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotlightCarousel(
    animes: List<AnimeMedia>,
    titleLang: String,
    onAnimeSelected: (AnimeMedia) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { animes.take(5).size })

    // Auto play every 5 seconds
    LaunchedEffect(key1 = pagerState) {
        while (true) {
            delay(5000)
            val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val anime = animes[page]
            val coverUrl = anime.bannerImage ?: anime.coverImage

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAnimeSelected(anime) }
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Spotlight Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Background gradient overlay: transparent -> black
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.4f to Color(0x66000000),
                                0.8f to Color(0xCC0F0F0F),
                                1.0f to Color(0xFF0F0F0F)
                            )
                        )
                )

                // Spotlight indicators and content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 30.dp)
                ) {
                    // Badge Score Custom overlay
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryAccent.copy(alpha = 0.9f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (anime.averageScore != null) "${anime.averageScore}% Score" else "No Score",
                            style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val titleText = when (titleLang) {
                        "english" -> anime.titleEnglish ?: anime.titleRomaji ?: "Anime"
                        "native" -> anime.titleNative ?: anime.titleRomaji ?: "Anime"
                        else -> anime.titleRomaji ?: anime.titleEnglish ?: "Anime"
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.displayLarge.copy(color = Color.White),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic pill features
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Format Pill
                        anime.format?.let { fmt ->
                            InfoTag(icon = Icons.Outlined.Tv, text = fmt)
                        }

                        // Episodes Pill
                        anime.episodes?.let { eps ->
                            InfoTag(icon = Icons.Outlined.VideoFile, text = "$eps Eps")
                        }

                        // Duration Pill
                        anime.duration?.let { dur ->
                            InfoTag(icon = Icons.Outlined.Timer, text = "$dur Min")
                        }
                    }
                }
            }
        }

        // Page bullet dots indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(pagerState.pageCount) { index ->
                val active = pagerState.currentPage == index
                val width by animateDpAsState(targetValue = if (active) 18.dp else 8.dp)
                val color = if (active) PrimaryAccent else Color.Gray.copy(alpha = 0.6f)

                Box(
                    modifier = Modifier
                        .size(width = width, height = 8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun InfoTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .background(Color(0x19FFFFFF))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
        )
    }
}

@Composable
fun DiscoverPromoCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        PrimaryAccent.copy(alpha = 0.25f),
                        PrimaryAccent.copy(alpha = 0.05f)
                    )
                )
            )
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Discover Anime",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Filter by genres, ratings, score, formats and more",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun HistorySection(
    historyList: List<WatchHistory>,
    onHistorySelected: (Int, Int) -> Unit
) {
    Column {
        Text(
            text = "Continue Watching",
            style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(historyList) { item ->
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .clickable { onHistorySelected(item.animeId, item.episodeNumber) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = "Anime cover",
                            modifier = Modifier
                                .size(width = 80.dp, height = 110.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = item.animeTitle,
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Medium),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Episode ${item.episodeNumber}",
                                style = MaterialTheme.typography.labelLarge.copy(color = PrimaryAccent, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Simple linear progress guide slider
                            val progress = if (item.durationSeconds > 0) {
                                item.progressSeconds.toFloat() / item.durationSeconds
                            } else {
                                0.3f
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = PrimaryAccent,
                                trackColor = Color.Gray.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeCard(
    anime: AnimeMedia,
    titleLang: String,
    onClick: () -> Unit
) {
    val titleText = when (titleLang) {
        "english" -> anime.titleEnglish ?: anime.titleRomaji ?: "Anime"
        "native" -> anime.titleNative ?: anime.titleRomaji ?: "Anime"
        else -> anime.titleRomaji ?: anime.titleEnglish ?: "Anime"
    }

    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .testTag("anime_card_${anime.id}")
    ) {
        Box(
            modifier = Modifier
                .width(135.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = anime.coverImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Rating Badge (Glassmorphic Top-Right)
            anime.averageScore?.let { score ->
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Dark subtle vignette gradient on card base
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )

            // Optional info format bottom sheet overlay inside card
            anime.format?.let { fmt ->
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = fmt,
                        style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontSize = 9.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = titleText,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val metaText = if (anime.episodes != null) "${anime.episodes} Episodes" else "Airing"
        Text(
            text = metaText,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
