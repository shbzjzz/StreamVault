package com.example.api

import com.example.model.MovieDetail
import com.example.model.MovieResponse
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TMDbService {

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("trending/tv/week")
    suspend fun getTrendingTVShows(
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("tv/on_the_air")
    suspend fun getOnAirTVShows(
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("with_genres") genreId: Int,
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("discover/tv")
    suspend fun discoverTVShows(
        @Query("with_genres") genreId: Int,
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("search/tv")
    suspend fun searchTVShows(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int
    ): MovieDetail

    @GET("tv/{id}")
    suspend fun getTVShowDetails(
        @Path("id") id: Int
    ): MovieDetail

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"
        private const val FALLBACK_API_KEY = "4e44d9029b1270a757cddc766a1bcb63"

        fun create(): TMDbService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            // Client interceptor to automatically add API Key and Language queries
            val apiKeyInterceptor = Interceptor { chain ->
                val originalRequest: Request = chain.request()
                val originalHttpUrl: HttpUrl = originalRequest.url

                val urlBuilder = originalHttpUrl.newBuilder()
                    .addQueryParameter("api_key", FALLBACK_API_KEY)
                    .addQueryParameter("language", "en-US")

                val requestBuilder: Request.Builder = originalRequest.newBuilder()
                    .url(urlBuilder.build())

                chain.proceed(requestBuilder.build())
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(apiKeyInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(TMDbService::class.java)
        }
    }
}
