package com.blackstack.beatbox

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Response data class for LRCLIB API
 */
data class LRCLIBResponse(
    val syncedLyrics: String?,
    val plainLyrics: String?
)

/**
 * Retrofit interface for LRCLIB API
 * Base URL: https://lrclib.net/api/
 */
interface LRCLIBApi {
    @GET("get")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): LRCLIBResponse
}

/**
 * Singleton object to provide configured Retrofit instance for LRCLIB API
 */
object LRCLIBApiClient {
    private const val BASE_URL = "https://lrclib.net/api/"
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 15L

    /**
     * Configured OkHttp client with timeouts and logging
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Configured Retrofit instance
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * API service instance
     */
    val api: LRCLIBApi by lazy {
        retrofit.create(LRCLIBApi::class.java)
    }
}
