package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale

class VeilwatchRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val historyDao = db.watchHistoryDao()
    private val watchlistDao = db.watchlistDao()
    private val prefDao = db.preferencesDao()

    private val aniListApi = NetworkClient.aniListApi
    private val jikanApi = NetworkClient.jikanApi
    private val aniSkipApi = NetworkClient.aniSkipApi
    private val hiAnimeApi = NetworkClient.hiAnimeApi

    // --- API TOKEN & AUTH PREFERENCES ---
    suspend fun getAniListToken(): String? {
        return prefDao.getPreference("anilist_token").map { it?.value }.firstOrNull()
    }

    suspend fun saveAniListToken(token: String) {
        prefDao.insertPreference(PreferenceItem("anilist_token", token))
    }

    suspend fun removeAniListToken() {
        prefDao.deletePreference("anilist_token")
    }

    suspend fun getPreferredTitleLang(): String {
        return prefDao.getPreference("title_lang").map { it?.value ?: "romaji" }.firstOrNull() ?: "romaji"
    }

    suspend fun savePreferredTitleLang(lang: String) {
        prefDao.insertPreference(PreferenceItem("title_lang", lang))
    }

    suspend fun getPreferredStreamServer(): String {
        return prefDao.getPreference("preferred_server").map { it?.value ?: "HD-1" }.firstOrNull() ?: "HD-1"
    }

    suspend fun savePreferredStreamServer(server: String) {
        prefDao.insertPreference(PreferenceItem("preferred_server", server))
    }

    suspend fun getPreferredStreamCategory(): String {
        return prefDao.getPreference("preferred_category").map { it?.value ?: "sub" }.firstOrNull() ?: "sub"
    }

    suspend fun savePreferredStreamCategory(category: String) {
        prefDao.insertPreference(PreferenceItem("preferred_category", category))
    }

    fun getAdultContentEnabled(): Flow<Boolean> {
        return prefDao.getPreference("adult_content").map { it?.value == "true" }
    }

    suspend fun saveAdultContentEnabled(enabled: Boolean) {
        prefDao.insertPreference(PreferenceItem("adult_content", enabled.toString()))
    }

    // --- LOCAL DATABASE API ---
    // History
    val watchHistory: Flow<List<WatchHistory>> = historyDao.getAllHistory()

    fun getHistoryForAnime(animeId: Int): Flow<WatchHistory?> = historyDao.getHistoryForAnime(animeId)

    suspend fun saveWatchHistory(
        animeId: Int,
        title: String,
        coverUrl: String,
        episode: Int,
        progress: Long,
        duration: Long
    ) {
        historyDao.insertHistory(
            WatchHistory(
                animeId = animeId,
                animeTitle = title,
                coverUrl = coverUrl,
                episodeNumber = episode,
                progressSeconds = progress,
                durationSeconds = duration
            )
        )
    }

    suspend fun clearHistory() = historyDao.clearHistory()

    // Query watchlist
    val localWatchlist: Flow<List<WatchlistItem>> = watchlistDao.getWatchlist()

    fun getLocalWatchlistItem(animeId: Int): Flow<WatchlistItem?> = watchlistDao.getWatchlistItem(animeId)

    suspend fun saveToLocalWatchlist(
        id: Int,
        title: String,
        coverUrl: String,
        status: String,
        score: Double,
        progress: Int
    ) {
        watchlistDao.insertWatchlist(
            WatchlistItem(
                id = id,
                animeId = id,
                animeTitle = title,
                coverUrl = coverUrl,
                status = status,
                score = score,
                progress = progress
            )
        )
    }

    suspend fun deleteFromLocalWatchlist(id: Int) = watchlistDao.deleteWatchlistItem(id)

    // --- ANILIST GRAPHQL ENDPOINTS ---
    suspend fun getTrendingAnime(page: Int = 1, perPage: Int = 20): List<AnimeMedia> {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                  id idMal type format status episodes duration isAdult
                  title { romaji english native userPreferred }
                  coverImage { extraLarge large medium color }
                  bannerImage description genres averageScore popularity
                  nextAiringEpisode { episode airingAt timeUntilAiring }
                  studios(isMain: true) { edges { node { id name } } }
                }
              }
            }
        """.trimIndent()

        val variables = mapOf("page" to page, "perPage" to perPage)
        val response = aniListApi.query(GraphQLRequest(query, variables))
        return response.parseMediaList()
    }

    suspend fun getPopularAnime(page: Int = 1, perPage: Int = 20): List<AnimeMedia> {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                  id idMal type format status episodes duration isAdult
                  title { romaji english native userPreferred }
                  coverImage { extraLarge large medium color }
                  bannerImage description genres averageScore popularity
                  nextAiringEpisode { episode airingAt timeUntilAiring }
                  studios(isMain: true) { edges { node { id name } } }
                }
              }
            }
        """.trimIndent()

        val variables = mapOf("page" to page, "perPage" to perPage)
        val response = aniListApi.query(GraphQLRequest(query, variables))
        return response.parseMediaList()
    }

    suspend fun searchAnime(
        search: String?,
        page: Int = 1,
        perPage: Int = 24,
        genres: List<String>? = null
    ): List<AnimeMedia> {
        val query = """
            query (${'$'}search: String, ${'$'}page: Int, ${'$'}perPage: Int, ${'$'}genres: [String]) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(search: ${'$'}search, type: ANIME, genre_in: ${'$'}genres, isAdult: false) {
                  id idMal type format status episodes duration isAdult
                  title { romaji english native userPreferred }
                  coverImage { extraLarge large medium color }
                  bannerImage description genres averageScore popularity
                }
              }
            }
        """.trimIndent()

        // Construct variables map filtering out null or empty
        val variables = mutableMapOf<String, Any?>("page" to page, "perPage" to perPage)
        if (!search.isNullOrEmpty()) variables["search"] = search
        if (!genres.isNullOrEmpty()) variables["genres"] = genres

        val response = aniListApi.query(GraphQLRequest(query, variables))
        return response.parseMediaList()
    }

    suspend fun getGenresCollection(): List<String> {
        val query = """
            query {
              GenreCollection
            }
        """.trimIndent()
        val response = aniListApi.query(GraphQLRequest(query))
        val data = response.data ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return (data["GenreCollection"] as? List<String>) ?: emptyList()
    }

    suspend fun getAnimeDetail(id: Int): AnimeDetail? {
        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id idMal type format status source episodes duration isAdult
                title { romaji english native userPreferred }
                coverImage { extraLarge large medium color }
                bannerImage description synonyms season seasonYear
                startDate { year month day }
                endDate { year month day }
                nextAiringEpisode { episode airingAt timeUntilAiring }
                genres averageScore meanScore popularity favourites
                tags { id name rank isGeneralSpoiler isMediaSpoiler }
                studios(isMain: true) { edges { node { id name } } }
                characters(sort: ROLE, perPage: 12) {
                  edges {
                    role node { id name { full } image { large medium } }
                  }
                }
                relations {
                  edges {
                    relationType node {
                      id type format status
                      title { userPreferred }
                      coverImage { large }
                    }
                  }
                }
                recommendations(sort: RATING_DESC, perPage: 10) {
                  nodes {
                    mediaRecommendation {
                      id title { userPreferred }
                      coverImage { large }
                      averageScore
                    }
                  }
                }
                rankings { id rank type format year season allTime context }
              }
            }
        """.trimIndent()

        val response = aniListApi.query(GraphQLRequest(query, mapOf("id" to id)))
        val data = response.data ?: return null
        val mediaMap = data["Media"] as? Map<String, Any?> ?: return null

        val media = mediaMap.parseAnimeMedia()

        val start = mediaMap["startDate"] as? Map<String, Any?>
        val startDateStr = start?.let {
            val yr = it["year"]
            val mth = it["month"]
            val dy = it["day"]
            if (yr != null && mth != null && dy != null) "$yr-$mth-$dy" else null
        }

        val end = mediaMap["endDate"] as? Map<String, Any?>
        val endDateStr = end?.let {
            val yr = it["year"]
            val mth = it["month"]
            val dy = it["day"]
            if (yr != null && mth != null && dy != null) "$yr-$mth-$dy" else null
        }

        // Parse Characters
        val charactersMap = mediaMap["characters"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val charEdges = charactersMap?.get("edges") as? List<Map<String, Any?>>
        val charactersList = charEdges?.mapNotNull { edge ->
            val role = edge["role"] as? String ?: "SUPPORTING"
            val node = edge["node"] as? Map<String, Any?> ?: return@mapNotNull null
            val charId = (node["id"] as? Number)?.toInt() ?: 0
            val nameMap = node["name"] as? Map<String, Any?>
            val charName = nameMap?.get("full") as? String ?: "Unknown"
            val imageMap = node["image"] as? Map<String, Any?>
            val imUrl = imageMap?.get("large") as? String ?: imageMap?.get("medium") as? String
            AnimeCharacter(charId, charName, role, imUrl)
        } ?: emptyList()

        // Parse Relations
        val relationsMap = mediaMap["relations"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val relEdges = relationsMap?.get("edges") as? List<Map<String, Any?>>
        val relationsList = relEdges?.mapNotNull { edge ->
            val relType = edge["relationType"] as? String ?: "OTHER"
            val node = edge["node"] as? Map<String, Any?> ?: return@mapNotNull null
            val relId = (node["id"] as? Number)?.toInt() ?: 0
            val type = node["type"] as? String ?: "ANIME"
            val coverMap = node["coverImage"] as? Map<String, Any?>
            val cov = coverMap?.get("large") as? String ?: ""
            val titleMap = node["title"] as? Map<String, Any?>
            val tStr = titleMap?.get("userPreferred") as? String ?: "Unknown"
            val fmt = node["format"] as? String
            val sts = node["status"] as? String
            AnimeRelation(relId, relType, type, tStr, cov, fmt, sts)
        } ?: emptyList()

        // Parse Recommendations
        val recMap = mediaMap["recommendations"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val recNodes = recMap?.get("nodes") as? List<Map<String, Any?>>
        val recList = recNodes?.mapNotNull { node ->
            val mediaRec = node["mediaRecommendation"] as? Map<String, Any?> ?: return@mapNotNull null
            val rId = (mediaRec["id"] as? Number)?.toInt() ?: return@mapNotNull null
            val titleMap = mediaRec["title"] as? Map<String, Any?>
            val tStr = titleMap?.get("userPreferred") as? String ?: "Unknown"
            val coverMap = mediaRec["coverImage"] as? Map<String, Any?>
            val cov = coverMap?.get("large") as? String ?: ""
            val score = (mediaRec["averageScore"] as? Number)?.toInt()
            AnimeMedia(
                id = rId, idMal = null, titleEnglish = tStr, titleRomaji = tStr, titleNative = null,
                format = null, status = null, description = null, coverImage = cov, bannerImage = null,
                averageScore = score, episodes = null, duration = null, season = null, seasonYear = null
            )
        } ?: emptyList()

        // Parse Rankings
        @Suppress("UNCHECKED_CAST")
        val rankingsNodes = mediaMap["rankings"] as? List<Map<String, Any?>>
        val rankingsList = rankingsNodes?.mapNotNull { rNode ->
            val context = rNode["context"] as? String ?: ""
            val rank = (rNode["rank"] as? Number)?.toInt() ?: 0
            "#$rank $context"
        } ?: emptyList()

        // Parse Tags
        @Suppress("UNCHECKED_CAST")
        val tagsNodes = mediaMap["tags"] as? List<Map<String, Any?>>
        val tagsList = tagsNodes?.mapNotNull { tNode ->
            tNode["name"] as? String
        } ?: emptyList()

        return AnimeDetail(
            media = media,
            startDate = startDateStr,
            endDate = endDateStr,
            characters = charactersList,
            relations = relationsList,
            recommendations = recList,
            rankings = rankingsList,
            tags = tagsList
        )
    }

    suspend fun getViewerProfile(): UserProfile? {
        val token = getAniListToken() ?: return null
        val query = """
            query {
              Viewer {
                id name about avatar { large medium }
                statistics {
                  anime { count meanScore minutesWatched episodesWatched }
                }
              }
            }
        """.trimIndent()

        val response = aniListApi.query(GraphQLRequest(query), "Bearer $token")
        val data = response.data ?: return null
        val viewer = data["Viewer"] as? Map<String, Any?> ?: return null

        val id = (viewer["id"] as? Number)?.toInt() ?: 0
        val name = viewer["name"] as? String ?: "Unknown User"
        val about = viewer["about"] as? String
        val avatarMap = viewer["avatar"] as? Map<String, Any?>
        val avatarUrl = avatarMap?.get("large") as? String ?: avatarMap?.get("medium") as? String

        val stats = viewer["statistics"] as? Map<String, Any?>
        val animeStats = stats?.get("anime") as? Map<String, Any?>
        val count = (animeStats?.get("count") as? Number)?.toInt() ?: 0
        val meanScore = (animeStats?.get("meanScore") as? Number)?.toDouble() ?: 0.0
        val watchedMinutes = (animeStats?.get("minutesWatched") as? Number)?.toInt() ?: 0
        val watchedEpisodes = (animeStats?.get("episodesWatched") as? Number)?.toInt() ?: 0

        return UserProfile(
            id = id, name = name, avatarUrl = avatarUrl, about = about,
            animeCount = count, meanScore = meanScore, episodesWatched = watchedEpisodes, minutesWatched = watchedMinutes
        )
    }

    suspend fun getViewerWatchlist(status: String? = "CURRENT"): List<WatchlistItem> {
        val token = getAniListToken() ?: return emptyList()
        val user = getViewerProfile() ?: return emptyList()

        val query = """
            query (${'$'}userId: Int, ${'$'}status: MediaListStatus, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                mediaList(userId: ${'$'}userId, type: ANIME, status: ${'$'}status) {
                  id status score progress
                  media {
                    id title { userPreferred romaji english }
                    coverImage { large }
                    episodes status
                  }
                }
              }
            }
        """.trimIndent()

        val variables = mapOf(
            "userId" to user.id,
            "status" to status,
            "page" to 1,
            "perPage" to 50
        )

        val response = aniListApi.query(GraphQLRequest(query, variables), "Bearer $token")
        val data = response.data ?: return emptyList()
        val page = data["Page"] as? Map<String, Any?> ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val mediaList = page["mediaList"] as? List<Map<String, Any?>> ?: return emptyList()

        return mediaList.mapNotNull { item ->
            val listId = (item["id"] as? Number)?.toInt() ?: 0
            val mStatus = item["status"] as? String ?: "CURRENT"
            val score = (item["score"] as? Number)?.toDouble() ?: 0.0
            val progress = (item["progress"] as? Number)?.toInt() ?: 0

            val media = item["media"] as? Map<String, Any?> ?: return@mapNotNull null
            val animeId = (media["id"] as? Number)?.toInt() ?: 0
            val titleMap = media["title"] as? Map<String, Any?>
            val aniTitle = titleMap?.get("userPreferred") as? String
                ?: titleMap?.get("english") as? String
                ?: titleMap?.get("romaji") as? String
                ?: "Unknown Title"
            val covMap = media["coverImage"] as? Map<String, Any?>
            val covUrl = covMap?.get("large") as? String ?: ""

            WatchlistItem(
                id = animeId,
                animeId = animeId,
                animeTitle = aniTitle,
                coverUrl = covUrl,
                status = mStatus,
                score = score,
                progress = progress,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    suspend fun saveWatchlistEntryToAniList(
        mediaId: Int,
        status: String,
        score: Double,
        progress: Int
    ): Boolean {
        val token = getAniListToken() ?: return false
        val mutation = """
            mutation SaveMediaListEntry(${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}score: Float, ${'$'}progress: Int) {
              SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, score: ${'$'}score, progress: ${'$'}progress) {
                id status score progress
              }
            }
        """.trimIndent()

        val variables = mapOf(
            "mediaId" to mediaId,
            "status" to status,
            "score" to score,
            "progress" to progress
        )

        return try {
            val response = aniListApi.query(GraphQLRequest(mutation, variables), "Bearer $token")
            response.data?.get("SaveMediaListEntry") != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleAniListFavourite(animeId: Int): Boolean {
        val token = getAniListToken() ?: return false
        val mutation = """
            mutation (${'$'}animeId: Int) {
              ToggleFavourite(animeId: ${'$'}animeId) {
                anime { nodes { id title { userPreferred } } }
              }
            }
        """.trimIndent()

        return try {
            val response = aniListApi.query(GraphQLRequest(mutation, mapOf("animeId" to animeId)), "Bearer $token")
            response.data?.get("ToggleFavourite") != null
        } catch (e: Exception) {
            false
        }
    }

    // --- MAL JIKAN API ENDPOINTS ---
    suspend fun getMALEpisodes(malId: Int): List<JikanEpisodeDto> {
        return try {
            jikanApi.getEpisodes(malId).data ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMALNews(malId: Int): List<JikanNewsDto> {
        return try {
            jikanApi.getNews(malId).data ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- ANISKIP EPISODE SKIP TIMES ---
    suspend fun getAniSkipTimes(malId: Int, episodeNum: Int, lengthSeconds: Int = 1440): List<AniSkipResult> {
        return try {
            val response = aniSkipApi.getSkipTimes(malId, episodeNum, episodeLength = lengthSeconds)
            response?.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- HI-ANIME STREAMING ENDPOINTS (WEB SCRAPER HELPER) ---
    suspend fun getHiAnimeHome(): HiAnimeHomeData? {
        return try {
            hiAnimeApi.getHome().data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchHiAnime(keyword: String, page: Int = 1): List<HiAnimeAnimeDto> {
        return try {
            hiAnimeApi.search(keyword, page).data?.animes ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHiAnimeInfo(animeId: String): HiAnimeDetailDto? {
        return try {
            hiAnimeApi.getInfo(animeId).data?.anime
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getHiAnimeEpisodes(animeId: String): List<HiAnimeEpisodeDto> {
        return try {
            hiAnimeApi.getEpisodes(animeId).data?.episodes ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHiAnimeServers(episodeId: String): HiAnimeServersData? {
        return try {
            hiAnimeApi.getServers(episodeId).data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getHiAnimeSources(episodeId: String, server: String, category: String): HiAnimeSourcesData? {
        return try {
            hiAnimeApi.getSources(episodeId, server, category).data
        } catch (e: Exception) {
            null
        }
    }

    // --- ANILIST API DATA EXTRACTION PARSING HELPERS ---
    private fun GraphQLResponse.parseMediaList(): List<AnimeMedia> {
        val data = this.data ?: return emptyList()
        val page = data["Page"] as? Map<String, Any?> ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val mediaList = page["media"] as? List<Map<String, Any?>> ?: return emptyList()
        return mediaList.map { it.parseAnimeMedia() }
    }

    private fun Map<String, Any?>.parseAnimeMedia(): AnimeMedia {
        val id = (this["id"] as? Number)?.toInt() ?: 0
        val idMal = (this["idMal"] as? Number)?.toInt()
        val title = this["title"] as? Map<String, Any?>
        val titleEnglish = title?.get("english") as? String
        val titleRomaji = title?.get("romaji") as? String
        val titleNative = title?.get("native") as? String

        val format = this["format"] as? String
        val status = this["status"] as? String
        val description = this["description"] as? String

        val coverImage = this["coverImage"] as? Map<String, Any?>
        val coverUrl = coverImage?.get("extraLarge") as? String ?: coverImage?.get("large") as? String ?: coverImage?.get("medium") as? String
        val bannerImage = this["bannerImage"] as? String
        val averageScore = (this["averageScore"] as? Number)?.toInt()
        val episodes = (this["episodes"] as? Number)?.toInt()
        val duration = (this["duration"] as? Number)?.toInt()

        @Suppress("UNCHECKED_CAST")
        val genres = (this["genres"] as? List<String>) ?: emptyList()
        val season = this["season"] as? String
        val seasonYear = (this["seasonYear"] as? Number)?.toInt()

        val nextAiring = this["nextAiringEpisode"] as? Map<String, Any?>
        val nextEp = (nextAiring?.get("episode") as? Number)?.toInt()
        val nextEpTime = (nextAiring?.get("airingAt") as? Number)?.toLong()

        val studios = this["studios"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val studiosEdges = studios?.get("edges") as? List<Map<String, Any?>>
        val studioName = studiosEdges?.firstOrNull()?.get("node")?.let { (it as? Map<String, Any?>)?.get("name") as? String }

        return AnimeMedia(
            id = id,
            idMal = idMal,
            titleEnglish = titleEnglish,
            titleRomaji = titleRomaji,
            titleNative = titleNative,
            format = format,
            status = status,
            description = description,
            coverImage = coverUrl,
            bannerImage = bannerImage,
            averageScore = averageScore,
            episodes = episodes,
            duration = duration,
            genres = genres,
            season = season,
            seasonYear = seasonYear,
            studioName = studioName,
            nextEpisodeNumber = nextEp,
            nextEpisodeAiringAt = nextEpTime
        )
    }
}
