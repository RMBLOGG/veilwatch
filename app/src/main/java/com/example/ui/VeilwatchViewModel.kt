package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class VeilwatchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VeilwatchRepository(application)

    // System Preferences State
    private val _titleLanguage = MutableStateFlow("romaji")
    val titleLanguage: StateFlow<String> = _titleLanguage.asStateFlow()

    private val _preferredServer = MutableStateFlow("HD-1")
    val preferredServer: StateFlow<String> = _preferredServer.asStateFlow()

    private val _preferredCategory = MutableStateFlow("sub") // "sub" or "dub"
    val preferredCategory: StateFlow<String> = _preferredCategory.asStateFlow()

    val adultContentEnabled: StateFlow<Boolean> = repository.getAdultContentEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Home / Feed States
    private val _trendingState = MutableStateFlow<UiState<List<AnimeMedia>>>(UiState.Loading)
    val trendingState: StateFlow<UiState<List<AnimeMedia>>> = _trendingState.asStateFlow()

    private val _popularState = MutableStateFlow<UiState<List<AnimeMedia>>>(UiState.Loading)
    val popularState: StateFlow<UiState<List<AnimeMedia>>> = _popularState.asStateFlow()

    private val _hiAnimeHomeState = MutableStateFlow<HiAnimeHomeData?>(null)
    val hiAnimeHomeState: StateFlow<HiAnimeHomeData?> = _hiAnimeHomeState.asStateFlow()

    // Search / Browse States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenres = MutableStateFlow<List<String>>(emptyList())
    val selectedGenres: StateFlow<List<String>> = _selectedGenres.asStateFlow()

    private val _searchResult = MutableStateFlow<UiState<List<AnimeMedia>>>(UiState.Success(emptyList()))
    val searchResult: StateFlow<UiState<List<AnimeMedia>>> = _searchResult.asStateFlow()

    private val _genreList = MutableStateFlow<List<String>>(emptyList())
    val genreList: StateFlow<List<String>> = _genreList.asStateFlow()

    // Anime Detail States
    private val _detailState = MutableStateFlow<UiState<AnimeDetail>>(UiState.Loading)
    val detailState: StateFlow<UiState<AnimeDetail>> = _detailState.asStateFlow()

    private val _malEpisodes = MutableStateFlow<List<JikanEpisodeDto>>(emptyList())
    val malEpisodes: StateFlow<List<JikanEpisodeDto>> = _malEpisodes.asStateFlow()

    private val _malNews = MutableStateFlow<List<JikanNewsDto>>(emptyList())
    val malNews: StateFlow<List<JikanNewsDto>> = _malNews.asStateFlow()

    // Watch / Video State
    private val _activeAnime = MutableStateFlow<AnimeMedia?>(null)
    val activeAnime: StateFlow<AnimeMedia?> = _activeAnime.asStateFlow()

    private val _activeEpisodes = MutableStateFlow<List<HiAnimeEpisodeDto>>(emptyList())
    val activeEpisodes: StateFlow<List<HiAnimeEpisodeDto>> = _activeEpisodes.asStateFlow()

    private val _currentPlayEpisode = MutableStateFlow<HiAnimeEpisodeDto?>(null)
    val currentPlayEpisode: StateFlow<HiAnimeEpisodeDto?> = _currentPlayEpisode.asStateFlow()

    private val _streamSources = MutableStateFlow<HiAnimeSourcesData?>(null)
    val streamSources: StateFlow<HiAnimeSourcesData?> = _streamSources.asStateFlow()

    private val _servers = MutableStateFlow<HiAnimeServersData?>(null)
    val servers: StateFlow<HiAnimeServersData?> = _servers.asStateFlow()

    private val _activeServer = MutableStateFlow("HD-1")
    val activeServer: StateFlow<String> = _activeServer.asStateFlow()

    private val _skipTimes = MutableStateFlow<List<AniSkipResult>>(emptyList())
    val skipTimes: StateFlow<List<AniSkipResult>> = _skipTimes.asStateFlow()

    // History and Watchlist Flows
    val localWatchlist: StateFlow<List<WatchlistItem>> = repository.localWatchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistory>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profile & Auth States
    private val _aniListToken = MutableStateFlow<String?>(null)
    val aniListToken: StateFlow<String?> = _aniListToken.asStateFlow()

    private val _userProfileState = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val userProfileState: StateFlow<UiState<UserProfile>> = _userProfileState.asStateFlow()

    // Sync watchlist with AniList if active
    private val _syncedWatchlistState = MutableStateFlow<List<WatchlistItem>>(emptyList())
    val syncedWatchlist: StateFlow<List<WatchlistItem>> = _syncedWatchlistState.asStateFlow()

    init {
        loadPreferences()
        refreshHomeFeed()
        loadGenres()

        // Debounced search reactive stream
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotEmpty() || _selectedGenres.value.isNotEmpty()) {
                        performSearch(query, _selectedGenres.value)
                    }
                }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _titleLanguage.value = repository.getPreferredTitleLang()
            _preferredServer.value = repository.getPreferredStreamServer()
            _preferredCategory.value = repository.getPreferredStreamCategory()
            _aniListToken.value = repository.getAniListToken()
            refreshUserProfile()
        }
    }

    fun loadGenres() {
        viewModelScope.launch {
            try {
                val list = repository.getGenresCollection()
                if (list.isNotEmpty()) {
                    _genreList.value = list
                } else {
                    _genreList.value = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Sci-Fi", "Romance", "Mystery", "Slice of Life", "Supernatural")
                }
            } catch (e: Exception) {
                _genreList.value = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Sci-Fi", "Romance", "Mystery", "Slice of Life", "Supernatural")
            }
        }
    }

    fun refreshHomeFeed() {
        viewModelScope.launch {
            _trendingState.value = UiState.Loading
            _popularState.value = UiState.Loading
            try {
                val trending = repository.getTrendingAnime(1, 12)
                _trendingState.value = UiState.Success(trending)
            } catch (e: Exception) {
                _trendingState.value = UiState.Error(e.message ?: "Failed to load trending anime.")
            }

            try {
                val popular = repository.getPopularAnime(1, 20)
                _popularState.value = UiState.Success(popular)
            } catch (e: Exception) {
                _popularState.value = UiState.Error(e.message ?: "Failed to load popular anime.")
            }

            try {
                val animeHome = repository.getHiAnimeHome()
                if (animeHome != null) {
                    _hiAnimeHomeState.value = animeHome
                }
            } catch (e: Exception) {
                // Ignore silent scraping fail
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleGenreFilter(genre: String) {
        val current = _selectedGenres.value.toMutableList()
        if (current.contains(genre)) {
            current.remove(genre)
        } else {
            current.add(genre)
        }
        _selectedGenres.value = current
        performSearch(_searchQuery.value, current)
    }

    fun clearSearchFilters() {
        _selectedGenres.value = emptyList()
        _searchQuery.value = ""
        _searchResult.value = UiState.Success(emptyList())
    }

    fun performSearch(query: String, genres: List<String>) {
        viewModelScope.launch {
            _searchResult.value = UiState.Loading
            try {
                val result = repository.searchAnime(
                    search = query.ifEmpty { null },
                    genres = genres.ifEmpty { null }
                )
                _searchResult.value = UiState.Success(result)
            } catch (e: Exception) {
                _searchResult.value = UiState.Error(e.message ?: "Search failed.")
            }
        }
    }

    fun loadAnimeDetail(id: Int) {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            _malEpisodes.value = emptyList()
            _malNews.value = emptyList()
            try {
                val detail = repository.getAnimeDetail(id)
                if (detail != null) {
                    _detailState.value = UiState.Success(detail)
                    _activeAnime.value = detail.media
                    
                    // Trigger MAL background loads using MAL id
                    detail.media.idMal?.let { malId ->
                        launch {
                            val eps = repository.getMALEpisodes(malId)
                            _malEpisodes.value = eps
                        }
                        launch {
                            val news = repository.getMALNews(malId)
                            _malNews.value = news
                        }
                    }

                    // Preload corresponding streaming matches over HiAnime
                    launch {
                        val keyword = detail.media.titleRomaji ?: detail.media.titleEnglish ?: ""
                        if (keyword.isNotEmpty()) {
                            val scrapeResults = repository.searchHiAnime(keyword)
                            val matched = scrapeResults.firstOrNull { result ->
                                result.name.equals(detail.media.titleRomaji, ignoreCase = true) ||
                                result.name.equals(detail.media.titleEnglish, ignoreCase = true)
                            } ?: scrapeResults.firstOrNull()

                            matched?.id?.let { matchedAnimeId ->
                                val eps = repository.getHiAnimeEpisodes(matchedAnimeId)
                                _activeEpisodes.value = eps
                            }
                        }
                    }
                } else {
                    _detailState.value = UiState.Error("Anime not found in AniList.")
                }
            } catch (e: Exception) {
                _detailState.value = UiState.Error(e.message ?: "Failed to get anime details.")
            }
        }
    }

    fun selectEpisodeToPlay(episode: HiAnimeEpisodeDto) {
        _currentPlayEpisode.value = episode
        _streamSources.value = null
        _servers.value = null
        _skipTimes.value = emptyList()

        viewModelScope.launch {
            // 1. Fetch servers
            val serversData = repository.getHiAnimeServers(episode.episodeId)
            _servers.value = serversData

            // 2. Fetch stream sources
            val prefSvr = _preferredServer.value
            val category = _preferredCategory.value
            val streams = repository.getHiAnimeSources(episode.episodeId, prefSvr, category)
            if (streams != null) {
                _streamSources.value = streams
            } else if (serversData != null) {
                // Fallback to whichever server exists
                val fallbackServer = serversData.sub?.firstOrNull()?.serverName ?: "HD-1"
                _activeServer.value = fallbackServer
                val fallbackStreams = repository.getHiAnimeSources(episode.episodeId, fallbackServer, category)
                _streamSources.value = fallbackStreams
            }

            // 3. Fetch skip times from Jikan match
            _activeAnime.value?.let { info ->
                info.idMal?.let { malId ->
                    val skips = repository.getAniSkipTimes(malId, episode.number)
                    _skipTimes.value = skips
                }
            }
        }
    }

    fun changeStreamingServer(serverName: String) {
        _activeServer.value = serverName
        val ep = _currentPlayEpisode.value ?: return
        viewModelScope.launch {
            _streamSources.value = null
            val streams = repository.getHiAnimeSources(ep.episodeId, serverName, _preferredCategory.value)
            _streamSources.value = streams
        }
    }

    fun toggleSubDub(category: String) { // "sub" or "dub"
        viewModelScope.launch {
            repository.savePreferredStreamCategory(category)
            _preferredCategory.value = category
            val ep = _currentPlayEpisode.value ?: return@launch
            _streamSources.value = null
            val streams = repository.getHiAnimeSources(ep.episodeId, _activeServer.value, category)
            _streamSources.value = streams
        }
    }

    // --- WATCHLIST & SYNC API IMPLEMENTATION ---
    fun updateWatchlist(anime: AnimeMedia, status: String, score: Double, progress: Int) {
        viewModelScope.launch {
            // Save locally
            repository.saveToLocalWatchlist(
                id = anime.id,
                title = anime.titleRomaji ?: anime.titleEnglish ?: "Anime",
                coverUrl = anime.coverImage ?: "",
                status = status,
                score = score,
                progress = progress
            )

            // Save to AniList if token exists
            if (_aniListToken.value != null) {
                repository.saveWatchlistEntryToAniList(anime.id, status, score, progress)
                refreshUserWatchlist()
            }
        }
    }

    fun toggleFavorite(animeId: Int) {
        viewModelScope.launch {
            if (_aniListToken.value != null) {
                repository.toggleAniListFavourite(animeId)
            }
        }
    }

    fun deleteWatchlistItem(id: Int) {
        viewModelScope.launch {
            repository.deleteFromLocalWatchlist(id)
            if (_aniListToken.value != null) {
                // In AniList, deletion can be done or updating to planning
                refreshUserWatchlist()
            }
        }
    }

    // --- WATCH HISTORY METRIC LOGGER ---
    fun logWatchHistory(animeId: Int, progressSeconds: Long, durationSeconds: Long) {
        val anime = _activeAnime.value ?: return
        val ep = _currentPlayEpisode.value?.number ?: 1
        viewModelScope.launch {
            repository.saveWatchHistory(
                animeId = animeId,
                title = anime.titleRomaji ?: anime.titleEnglish ?: "Anime",
                coverUrl = anime.coverImage ?: "",
                episode = ep,
                progress = progressSeconds,
                duration = durationSeconds
            )
        }
    }

    fun deleteHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // --- SETTINGS PREFERENCES TRIGGERS ---
    fun updateTitleLang(lang: String) {
        viewModelScope.launch {
            repository.savePreferredTitleLang(lang)
            _titleLanguage.value = lang
        }
    }

    fun updateAdultContent(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAdultContentEnabled(enabled)
        }
    }

    // --- ANILIST AUTH LOGINS ---
    fun authenticateAniList(token: String) {
        viewModelScope.launch {
            repository.saveAniListToken(token)
            _aniListToken.value = token
            refreshUserProfile()
            refreshUserWatchlist()
        }
    }

    fun logoutAniList() {
        viewModelScope.launch {
            repository.removeAniListToken()
            _aniListToken.value = null
            _userProfileState.value = UiState.Error("Logged Out")
            _syncedWatchlistState.value = emptyList()
        }
    }

    fun refreshUserProfile() {
        val token = _aniListToken.value
        if (token == null) {
            _userProfileState.value = UiState.Error("Anilist account is not authenticated.")
            return
        }
        viewModelScope.launch {
            _userProfileState.value = UiState.Loading
            try {
                val profile = repository.getViewerProfile()
                if (profile != null) {
                    _userProfileState.value = UiState.Success(profile)
                } else {
                    _userProfileState.value = UiState.Error("Failed to parse Viewer Profile")
                }
            } catch (e: Exception) {
                _userProfileState.value = UiState.Error(e.message ?: "Authentication expired")
            }
        }
    }

    fun refreshUserWatchlist() {
        if (_aniListToken.value == null) return
        viewModelScope.launch {
            try {
                val activeList = repository.getViewerWatchlist("CURRENT")
                val completedList = repository.getViewerWatchlist("COMPLETED")
                val planningList = repository.getViewerWatchlist("PLANNING")
                val onHoldList = repository.getViewerWatchlist("ON_HOLD")
                val droppedList = repository.getViewerWatchlist("DROPPED")

                val totalList = activeList + completedList + planningList + onHoldList + droppedList
                _syncedWatchlistState.value = totalList

                // Synchronize locally as well
                totalList.forEach { item ->
                    repository.saveToLocalWatchlist(
                        id = item.id,
                        title = item.animeTitle,
                        coverUrl = item.coverUrl,
                        status = item.status,
                        score = item.score,
                        progress = item.progress
                    )
                }
            } catch (e: Exception) {
                // Fallback to local on connection drop
            }
        }
    }
}
