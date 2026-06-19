package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MovieResponse(
    val results: List<MediaItem>,
    @Json(name = "total_pages") val totalPages: Int? = null,
    @Json(name = "total_results") val totalResults: Int? = null
)

@JsonClass(generateAdapter = true)
data class MediaItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    val popularity: Double? = 0.0,
    var selectedType: String? = null // local helper
) {
    val displayTitle: String get() = title ?: name ?: "Untitled"
    val releaseYear: String get() = (releaseDate ?: firstAirDate ?: "").split("-").firstOrNull() ?: ""
    val ratingString: String get() = voteAverage?.let { String.format("%.1f", it) } ?: ""
    val itemType: String get() = selectedType ?: if (title != null) "movie" else "tv"
}

@JsonClass(generateAdapter = true)
data class MovieDetail(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    val genres: List<Genre>? = null,
    val runtime: Int? = null,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int? = null,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int? = null,
    val seasons: List<TVSeason>? = null
) {
    val displayTitle: String get() = title ?: name ?: "Untitled"
    val releaseYear: String get() = (releaseDate ?: firstAirDate ?: "").split("-").firstOrNull() ?: ""
    val ratingString: String get() = voteAverage?.let { String.format("%.1f", it) } ?: ""
    val itemType: String get() = if (title != null) "movie" else "tv"
}

@JsonClass(generateAdapter = true)
data class Genre(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TVSeason(
    val id: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "episode_count") val episodeCount: Int,
    val name: String? = null
)
