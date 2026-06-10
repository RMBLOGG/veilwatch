package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// GraphQL Standard Models
@JsonClass(generateAdapter = true)
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class GraphQLResponse(
    val data: Map<String, Any?>? = null,
    val errors: List<GraphQLError>? = null
)

@JsonClass(generateAdapter = true)
data class GraphQLError(
    val message: String
)

// Simplified domain representations constructed from GraphQL/REST responses
data class AnimeMedia(
    val id: Int,
    val idMal: Int?,
    val titleEnglish: String?,
    val titleRomaji: String?,
    val titleNative: String?,
    val format: String?,
    val status: String?,
    val description: String?,
    val coverImage: String?,
    val bannerImage: String?,
    val averageScore: Int?,
    val episodes: Int?,
    val duration: Int?,
    val genres: List<String> = emptyList(),
    val season: String?,
    val seasonYear: Int?,
    val studioName: String? = null,
    val nextEpisodeNumber: Int? = null,
    val nextEpisodeAiringAt: Long? = null
)

// AniList Detail Representation
data class AnimeDetail(
    val media: AnimeMedia,
    val startDate: String?,
    val endDate: String?,
    val characters: List<AnimeCharacter> = emptyList(),
    val relations: List<AnimeRelation> = emptyList(),
    val recommendations: List<AnimeMedia> = emptyList(),
    val rankings: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

data class AnimeCharacter(
    val id: Int,
    val name: String,
    val role: String,
    val imageUrl: String?
)

data class AnimeRelation(
    val id: Int,
    val relationType: String,
    val type: String,
    val title: String,
    val coverUrl: String,
    val format: String?,
    val status: String?
)

// AniList User Profile Representation
data class UserProfile(
    val id: Int,
    val name: String,
    val avatarUrl: String?,
    val about: String?,
    val animeCount: Int,
    val meanScore: Double,
    val episodesWatched: Int,
    val minutesWatched: Int
)

// Jikan (MAL) API representation
@JsonClass(generateAdapter = true)
data class JikanEpisodesResponse(
    val data: List<JikanEpisodeDto>?
)

@JsonClass(generateAdapter = true)
data class JikanEpisodeDto(
    @Json(name = "mal_id") val malId: Int,
    val title: String?,
    @Json(name = "episode") val episode: String?,
    val filler: Boolean?,
    val recap: Boolean?
)

@JsonClass(generateAdapter = true)
data class JikanNewsResponse(
    val data: List<JikanNewsDto>?
)

@JsonClass(generateAdapter = true)
data class JikanNewsDto(
    val title: String?,
    val date: String?,
    val author_username: String?,
    val url: String?,
    val excerpt: String?,
    val images: JikanNewsImagesDto?
)

@JsonClass(generateAdapter = true)
data class JikanNewsImagesDto(
    val jpg: JikanNewsImageDetailDto?
)

@JsonClass(generateAdapter = true)
data class JikanNewsImageDetailDto(
    val image_url: String?
)

// AniSkip API representation
@JsonClass(generateAdapter = true)
data class AniSkipResponse(
    val results: List<AniSkipResult>?
)

@JsonClass(generateAdapter = true)
data class AniSkipResult(
    val interval: AniSkipInterval?,
    @Json(name = "skipType") val skipType: String? // "op" or "ed"
)

@JsonClass(generateAdapter = true)
data class AniSkipInterval(
    @Json(name = "startTime") val startTime: Double,
    @Json(name = "endTime") val endTime: Double
)

// HiAnime Scraper API representations (Vercel)
@JsonClass(generateAdapter = true)
data class HiAnimeHomeResponse(
    val success: Boolean = false,
    val data: HiAnimeHomeData? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeHomeData(
    val spotlightAnimes: List<HiAnimeAnimeDto>? = null,
    val trendingAnimes: List<HiAnimeAnimeDto>? = null,
    val latestEpisodeAnimes: List<HiAnimeAnimeDto>? = null,
    val topAiringAnimes: List<HiAnimeAnimeDto>? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeAnimeDto(
    val id: String,
    val name: String,
    val poster: String?,
    val duration: String?,
    val type: String?,
    val rating: String?,
    val episodes: Map<String, Any?>? = null // sometimes contains sub/dub count
)

@JsonClass(generateAdapter = true)
data class HiAnimeSearchResponse(
    val success: Boolean = false,
    val data: HiAnimeSearchData? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeSearchData(
    val animes: List<HiAnimeAnimeDto>? = null,
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false
)

@JsonClass(generateAdapter = true)
data class HiAnimeInfoResponse(
    val success: Boolean = false,
    val data: HiAnimeInfoData? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeInfoData(
    val anime: HiAnimeDetailDto? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeDetailDto(
    val info: HiAnimeInfoDto? = null,
    val moreInfo: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeInfoDto(
    val id: String,
    val name: String,
    val poster: String?,
    val description: String?,
    val stats: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeEpisodesResponse(
    val success: Boolean = false,
    val data: HiAnimeEpisodesData? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeEpisodesData(
    val totalEpisodes: Int = 0,
    val episodes: List<HiAnimeEpisodeDto>? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeEpisodeDto(
    val title: String?,
    val episodeId: String, // format: "shingeki-no-kyojin-1?ep=233"
    val number: Int
)

@JsonClass(generateAdapter = true)
data class HiAnimeServersResponse(
    val success: Boolean = false,
    val data: HiAnimeServersData? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeServersData(
    val sub: List<HiAnimeServerDto>? = null,
    val dub: List<HiAnimeServerDto>? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeServerDto(
    val serverId: Int,
    val serverName: String
)

@JsonClass(generateAdapter = true)
data class HiAnimeSourcesResponse(
    val success: Boolean = false,
    val data: HiAnimeSourcesData? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeSourcesData(
    val sources: List<HiAnimeSourceFileDto>? = null,
    val tracks: List<HiAnimeTrackDto>? = null,
    val intro: HiAnimeSkipIntervalDto? = null,
    val outro: HiAnimeSkipIntervalDto? = null
)

@JsonClass(generateAdapter = true)
data class HiAnimeSourceFileDto(
    val url: String, // Streaming M3U8 source url!
    val type: String // e.g. "hls"
)

@JsonClass(generateAdapter = true)
data class HiAnimeTrackDto(
    val file: String, // Subtitle/Caption url
    val label: String?, // English, Spanish, etc.
    val kind: String? // "captions", "thumbnails"
)

@JsonClass(generateAdapter = true)
data class HiAnimeSkipIntervalDto(
    val start: Double,
    val end: Double
)
