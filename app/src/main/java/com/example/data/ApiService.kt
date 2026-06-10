package com.example.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface AniListApiService {
    @POST("https://graphql.anilist.co/")
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun query(
        @Body request: GraphQLRequest,
        @Header("Authorization") token: String? = null
    ): GraphQLResponse
}

interface JikanApiService {
    @GET("anime/{malId}/episodes")
    suspend fun getEpisodes(
        @Path("malId") malId: Int,
        @Query("page") page: Int = 1
    ): JikanEpisodesResponse

    @GET("anime/{malId}/news")
    suspend fun getNews(
        @Path("malId") malId: Int
    ): JikanNewsResponse
}

interface AniSkipApiService {
    @GET("skip-times/{malId}/{episodeNumber}")
    suspend fun getSkipTimes(
        @Path("malId") malId: Int,
        @Path("episodeNumber") episodeNumber: Int,
        @Query("types[]") types: List<String> = listOf("op", "ed", "mixed-op", "mixed-ed", "recap"),
        @Query("episodeLength") episodeLength: Int = 1440 // 24 mins default length in seconds
    ): AniSkipResponse?
}

interface HiAnimeApiService {
    @GET("home")
    suspend fun getHome(): HiAnimeHomeResponse

    @GET("search")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1
    ): HiAnimeSearchResponse

    @GET("info/{animeId}")
    suspend fun getInfo(
        @Path("animeId") animeId: String
    ): HiAnimeInfoResponse

    @GET("episodes/{animeId}")
    suspend fun getEpisodes(
        @Path("animeId") animeId: String
    ): HiAnimeEpisodesResponse

    @GET("episode/servers")
    suspend fun getServers(
        @Query("episodeId") episodeId: String
    ): HiAnimeServersResponse

    @GET("episode/sources")
    suspend fun getSources(
        @Query("id") episodeId: String,
        @Query("server") server: String,
        @Query("category") category: String // "sub" or "dub"
    ): HiAnimeSourcesResponse
}

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
            .build()
        return chain.proceed(request)
    }
}

object NetworkClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    val aniListApi: AniListApiService by lazy {
        createRetrofit("https://graphql.anilist.co/").create(AniListApiService::class.java)
    }

    val jikanApi: JikanApiService by lazy {
        createRetrofit("https://api.jikan.moe/v4/").create(JikanApiService::class.java)
    }

    val aniSkipApi: AniSkipApiService by lazy {
        createRetrofit("https://api.aniskip.com/v2/").create(AniSkipApiService::class.java)
    }

    val hiAnimeApi: HiAnimeApiService by lazy {
        createRetrofit("https://shonenx-aniwatch-instance.vercel.app/api/v2/hianime/").create(HiAnimeApiService::class.java)
    }
}
