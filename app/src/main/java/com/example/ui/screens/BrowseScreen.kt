package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import com.example.data.AnimeMedia
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UiState
import com.example.ui.VeilwatchViewModel
import com.example.ui.theme.*

@Composable
fun BrowseScreen(
    viewModel: VeilwatchViewModel,
    onNavigateToDetail: (Int) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val selectedGenres by viewModel.selectedGenres.collectAsState()
    val genreList by viewModel.genreList.collectAsState()
    val titleLang by viewModel.titleLanguage.collectAsState()

    var showFilters by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Text(
            text = "Browse & Search",
            style = MaterialTheme.typography.displayMedium.copy(color = Color.White),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom High Fidelity Search Field
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search for anime titles...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryAccent) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    disabledContainerColor = DarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .testTag("search_input")
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Filters Trigger Button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (showFilters) PrimaryAccent else DarkSurface)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .clickable { showFilters = !showFilters }
                    .testTag("filter_trigger"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = if (showFilters) Color.White else PrimaryAccent
                )
            }
        }

        // Horizontal Genres selection pill row
        AnimatedVisibility(visible = showFilters) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Filter by Genre",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genreList) { genre ->
                        val isSelected = selectedGenres.contains(genre)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryAccent else Color(0x1AFFFFFF))
                                .border(1.dp, if (isSelected) PrimaryAccent else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleGenreFilter(genre) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Clear button
                if (selectedGenres.isNotEmpty() || searchQuery.isNotEmpty()) {
                    Text(
                        text = "Reset all configurations",
                        style = MaterialTheme.typography.labelMedium.copy(color = SecondaryAccent),
                        modifier = Modifier
                            .clickable { viewModel.clearSearchFilters() }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid result layout
        when (val state = searchResult) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Start searching for your favorites!",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Or toggle filters above to narrow your collection.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("search_grid")
                    ) {
                        items(state.data) { anime ->
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                AnimeGridCard(
                                    anime = anime,
                                    titleLang = titleLang,
                                    onClick = { onNavigateToDetail(anime.id) }
                                )
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Search Error: ${state.message}", color = SecondaryAccent)
                }
            }
        }
    }
}

@Composable
fun AnimeGridCard(
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
            .width(155.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(155.dp)
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x1BFFFFFF), RoundedCornerShape(16.dp))
        ) {
            // Main image
            coil.compose.AsyncImage(
                model = anime.coverImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Star score badge (floating top right)
            anime.averageScore?.let { score ->
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = " $score%",
                            style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = titleText,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = anime.genres.take(2).joinToString(", "),
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
