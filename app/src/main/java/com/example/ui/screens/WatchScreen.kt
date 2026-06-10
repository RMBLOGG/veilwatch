package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView
import com.example.data.HiAnimeEpisodeDto
import com.example.ui.VeilwatchViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun WatchScreen(
    animeId: Int,
    episodeNum: Int,
    viewModel: VeilwatchViewModel,
    onNavigateBack: () -> Unit
) {
    val activeAnime by viewModel.activeAnime.collectAsState()
    val activeEpisodes by viewModel.activeEpisodes.collectAsState()
    val currentPlayEpisode by viewModel.currentPlayEpisode.collectAsState()
    val streamSources by viewModel.streamSources.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val activeServer by viewModel.activeServer.collectAsState()
    val preferredCategory by viewModel.preferredCategory.collectAsState()
    val skipTimes by viewModel.skipTimes.collectAsState()

    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentPlaybackSec by remember { mutableLongStateOf(0L) }
    var durationSec by remember { mutableLongStateOf(0L) }

    // Synchronize play state if loaded episode mismatches requested
    LaunchedEffect(key1 = episodeNum, key2 = activeEpisodes) {
        val requestedEp = activeEpisodes.firstOrNull { it.number == episodeNum }
            ?: activeEpisodes.firstOrNull()
        requestedEp?.let { viewModel.selectEpisodeToPlay(it) }
    }

    // Launch ExoPlayer instance
    DisposableEffect(streamSources) {
        val streamUrl = streamSources?.sources?.firstOrNull()?.url
        if (streamUrl != null) {
            val player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                
                // Configure HLS Stream Source
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.Builder().setUri(streamUrl).setMimeType(MimeTypes.APPLICATION_M3U8).build())

                setMediaSource(hlsMediaSource)
                prepare()
            }
            exoPlayer = player
        }

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // Interval logging watch progress ticks & AniSkip trigger polling
    LaunchedEffect(exoPlayer) {
        while (exoPlayer != null) {
            val player = exoPlayer ?: break
            currentPlaybackSec = player.currentPosition / 1000
            durationSec = player.duration / 1000
            
            if (currentPlaybackSec > 0 && durationSec > 0 && currentPlaybackSec % 6 == 0L) {
                // Log watch history
                viewModel.logWatchHistory(animeId, currentPlaybackSec, durationSec)
            }
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Video Space Card element
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.Black)
                .testTag("video_container"),
            contentAlignment = Alignment.Center
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Skip Intro/Outro Overlap Button matching AniSkip interval seconds
                val matchingSkip = skipTimes.firstOrNull { skip ->
                    skip.interval?.let { interval ->
                        currentPlaybackSec >= interval.startTime && currentPlaybackSec <= interval.endTime
                    } ?: false
                }

                if (matchingSkip != null) {
                    val label = if (matchingSkip.skipType == "op") "Skip Opening" else "Skip Content"
                    Button(
                        onClick = {
                            matchingSkip.interval?.endTime?.let { targetTime ->
                                exoPlayer?.seekTo((targetTime * 1000).toLong())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 60.dp, end = 20.dp)
                            .testTag("skip_button")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(label, color = Color.White)
                    }
                }
            } else {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        }

        // Active details info block
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeAnime?.titleRomaji ?: "Streaming Player",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Episode ${currentPlayEpisode?.number ?: 1}: ${currentPlayEpisode?.title ?: "Original video"}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = PrimaryAccent, fontWeight = FontWeight.SemiBold)
                    )
                }

                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DarkCard)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close player", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub/Dub category Selector pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Tracks: ", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                listOf("sub", "dub").forEach { cat ->
                    val active = preferredCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) PrimaryAccent else DarkCard)
                            .clickable { viewModel.toggleSubDub(cat) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cat.uppercase(),
                            color = if (active) Color.White else TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Servers Row lists
            if (servers != null) {
                Text(text = "Stream Servers", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val serverList = if (preferredCategory == "sub") servers?.sub else servers?.dub
                    serverList?.forEach { svr ->
                        val isSelected = activeServer == svr.serverName
                        Box(
                            value = svr.serverName,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryAccent else Color(0x1AFFFFFF))
                                .border(1.dp, if (isSelected) PrimaryAccent else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { viewModel.changeStreamingServer(svr.serverName) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = svr.serverName,
                                color = if (isSelected) Color.White else TextSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Episode selection drawer footer
        Text(
            text = "Episodes Pick",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(activeEpisodes) { ep ->
                val playing = currentPlayEpisode?.number == ep.number
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (playing) PrimaryAccent.copy(alpha = 0.15f) else DarkCard)
                        .clickable { viewModel.selectEpisodeToPlay(ep) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (playing) Icons.Default.PlayArrow else Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = if (playing) PrimaryAccent else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = ep.title ?: "Episode ${ep.number}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (playing) PrimaryAccent else Color.White,
                                    fontWeight = if (playing) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                        if (playing) {
                            Text(
                                text = "NOW PLAYING",
                                color = PrimaryAccent,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom wrapper helper modifier extension parameter
@Composable
private fun Box(
    value: String,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier, content = content)
}
