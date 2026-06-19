package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.TMDbService
import com.example.model.MediaItem
import com.example.model.MovieDetail
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class StreamingProvider(
    val label: String,
    val note: String,
    val getMovieUrl: (Int) -> String,
    val getTVUrl: (Int, Int, Int) -> String
)

val PROVIDERS = listOf(
    StreamingProvider(
        "VidLink", "Low ads",
        { id -> "https://vidlink.pro/movie/$id" },
        { id, s, e -> "https://vidlink.pro/tv/$id/$s/$e" }
    ),
    StreamingProvider(
        "VidSrc Pro", "HD",
        { id -> "https://vidsrc.pro/embed/movie/$id" },
        { id, s, e -> "https://vidsrc.pro/embed/tv?tmdb=$id&season=$s&episode=$e" }
    ),
    StreamingProvider(
        "VidSrc.fyi", "Multi-CDN",
        { id -> "https://vidsrc.fyi/embed/movie/$id" },
        { id, s, e -> "https://vidsrc.fyi/embed/tv/$id/$s/$e" }
    ),
    StreamingProvider(
        "2Embed", "Fast",
        { id -> "https://www.2embed.cc/embed/$id" },
        { id, s, e -> "https://www.2embed.cc/embedtv/$id&s=$s&e=$e" }
    ),
    StreamingProvider(
        "AutoEmbed", "Auto",
        { id -> "https://player.autoembed.cc/embed/movie/$id" },
        { id, s, e -> "https://player.autoembed.cc/embed/tv/$id/$s/$e" }
    ),
    StreamingProvider(
        "VidBinge", "Clean",
        { id -> "https://vidbinge.dev/embed/movie/$id" },
        { id, s, e -> "https://vidbinge.dev/embed/tv/$id/$s/$e" }
    )
)

data class GenreItem(val id: Int, val name: String)

val GENRES_LIST = listOf(
    GenreItem(0, "All"),
    GenreItem(28, "Action"),
    GenreItem(18, "Drama"),
    GenreItem(35, "Comedy"),
    GenreItem(53, "Thriller"),
    GenreItem(27, "Horror"),
    GenreItem(878, "Sci-Fi"),
    GenreItem(10749, "Romance"),
    GenreItem(16, "Animation"),
    GenreItem(99, "Documentary")
)

class MediaViewModel(private val apiService: TMDbService = TMDbService.create()) : ViewModel() {

    // Tab Selection: "Home", "Series", "Movies"
    private val _activeTab = MutableStateFlow("Home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Genre Selection
    private val _selectedGenreId = MutableStateFlow(0)
    val selectedGenreId: StateFlow<Int> = _selectedGenreId.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Home Section states
    private val _trendingMovies = MutableStateFlow<UiState<List<MediaItem>>>(UiState.Loading)
    val trendingMovies = _trendingMovies.asStateFlow()

    private val _trendingTVShows = MutableStateFlow<UiState<List<MediaItem>>>(UiState.Loading)
    val trendingTVShows = _trendingTVShows.asStateFlow()

    private val _nowPlayingMovies = MutableStateFlow<UiState<List<MediaItem>>>(UiState.Loading)
    val nowPlayingMovies = _nowPlayingMovies.asStateFlow()

    private val _onAirTVShows = MutableStateFlow<UiState<List<MediaItem>>>(UiState.Loading)
    val onAirTVShows = _onAirTVShows.asStateFlow()

    private val _topRatedMovies = MutableStateFlow<UiState<List<MediaItem>>>(UiState.Loading)
    val topRatedMovies = _topRatedMovies.asStateFlow()

    // Grid Discover states
    private val _discoverItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val discoverItems = _discoverItems.asStateFlow()

    private val _isDiscoverLoading = MutableStateFlow(false)
    val isDiscoverLoading = _isDiscoverLoading.asStateFlow()

    private var currentDiscoverPage = 1
    private var isCategoryEnd = false

    // Search outcome lists
    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // Info modal items
    private val _selectedMedia = MutableStateFlow<MediaItem?>(null)
    val selectedMedia = _selectedMedia.asStateFlow()

    private val _selectedMediaDetail = MutableStateFlow<MovieDetail?>(null)
    val selectedMediaDetail = _selectedMediaDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail = _isLoadingDetail.asStateFlow()

    // active Player states
    private val _playingMedia = MutableStateFlow<MediaItem?>(null)
    val playingMedia = _playingMedia.asStateFlow()

    private val _playerSeason = MutableStateFlow(1)
    val playerSeason = _playerSeason.asStateFlow()

    private val _playerEpisode = MutableStateFlow(1)
    val playerEpisode = _playerEpisode.asStateFlow()

    private val _playerProviderIndex = MutableStateFlow(0)
    val playerProviderIndex = _playerProviderIndex.asStateFlow()

    private val _playerUrl = MutableStateFlow("")
    val playerUrl = _playerUrl.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs = _consoleLogs.asStateFlow()

    fun addConsoleLog(log: String) {
        _consoleLogs.update { current ->
            (current + log).takeLast(150)
        }
    }

    fun clearConsoleLogs() {
        _consoleLogs.value = emptyList()
    }

    init {
        loadHomeData()

        // Handle live search
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }
    }

    fun selectTab(tab: String) {
        _activeTab.value = tab
        // Reset query and discovery
        _searchQuery.value = ""
        if (tab != "Home") {
            currentDiscoverPage = 1
            _discoverItems.value = emptyList()
            isCategoryEnd = false
        }
    }

    fun selectGenre(genreId: Int) {
        _selectedGenreId.value = genreId
        if (_activeTab.value == "Home") {
            // Clicking genre on Home takes user to Movies tab by default
            _activeTab.value = "Movies"
        }
        currentDiscoverPage = 1
        _discoverItems.value = emptyList()
        isCategoryEnd = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMediaItem(item: MediaItem?) {
        _selectedMedia.value = item
        _selectedMediaDetail.value = null
        if (item != null) {
            loadMediaDetails(item)
        }
    }

    fun startPlaying(item: MediaItem) {
        _playingMedia.value = item
        _playerSeason.value = 1
        _playerEpisode.value = 1
        _playerProviderIndex.value = 0
        updatePlayerUrl()
    }

    fun stopPlaying() {
        _playingMedia.value = null
        _playerUrl.value = ""
    }

    fun setPlayerSeason(season: Int, epCount: Int) {
        _playerSeason.value = season
        _playerEpisode.value = 1
        updatePlayerUrl()
    }

    fun setPlayerEpisode(episode: Int) {
        _playerEpisode.value = episode
        updatePlayerUrl()
    }

    fun setPlayerProvider(index: Int) {
        _playerProviderIndex.value = index
        updatePlayerUrl()
    }

    private fun updatePlayerUrl() {
        val media = _playingMedia.value ?: return
        val provider = PROVIDERS.getOrNull(_playerProviderIndex.value) ?: PROVIDERS[0]
        val type = media.itemType

        _playerUrl.value = if (type == "tv") {
            provider.getTVUrl(media.id, _playerSeason.value, _playerEpisode.value)
        } else {
            provider.getMovieUrl(media.id)
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _trendingMovies.value = UiState.Loading
            _trendingTVShows.value = UiState.Loading
            _nowPlayingMovies.value = UiState.Loading
            _onAirTVShows.value = UiState.Loading
            _topRatedMovies.value = UiState.Loading

            launch {
                try {
                    val resp = apiService.getTrendingMovies(1)
                    val items = resp.results.onEach { it.selectedType = "movie" }
                    _trendingMovies.value = UiState.Success(items)
                } catch (e: Exception) {
                    _trendingMovies.value = UiState.Error(e.message ?: "Failed loading trending movies")
                }
            }

            launch {
                try {
                    val resp = apiService.getTrendingTVShows(1)
                    val items = resp.results.onEach { it.selectedType = "tv" }
                    _trendingTVShows.value = UiState.Success(items)
                } catch (e: Exception) {
                    _trendingTVShows.value = UiState.Error(e.message ?: "Failed loading trending TV shows")
                }
            }

            launch {
                try {
                    val resp = apiService.getNowPlayingMovies(1)
                    val items = resp.results.onEach { it.selectedType = "movie" }
                    _nowPlayingMovies.value = UiState.Success(items)
                } catch (e: Exception) {
                    _nowPlayingMovies.value = UiState.Error(e.message ?: "Failed loading now playing movies")
                }
            }

            launch {
                try {
                    val resp = apiService.getOnAirTVShows(1)
                    val items = resp.results.onEach { it.selectedType = "tv" }
                    _onAirTVShows.value = UiState.Success(items)
                } catch (e: Exception) {
                    _onAirTVShows.value = UiState.Error(e.message ?: "Failed loading on air TV shows")
                }
            }

            launch {
                try {
                    val resp = apiService.getTopRatedMovies(1)
                    val items = resp.results.onEach { it.selectedType = "movie" }
                    _topRatedMovies.value = UiState.Success(items)
                } catch (e: Exception) {
                    _topRatedMovies.value = UiState.Error(e.message ?: "Failed loading top rated movies")
                }
            }
        }
    }

    fun loadDiscoverData() {
        if (_isDiscoverLoading.value || isCategoryEnd) return
        val isTV = _activeTab.value == "Series"
        val genre = _selectedGenreId.value

        _isDiscoverLoading.value = true
        viewModelScope.launch {
            try {
                val response = if (isTV) {
                    if (genre > 0) apiService.discoverTVShows(genre, currentDiscoverPage)
                    else apiService.getTrendingTVShows(currentDiscoverPage)
                } else {
                    if (genre > 0) apiService.discoverMovies(genre, currentDiscoverPage)
                    else apiService.getTrendingMovies(currentDiscoverPage)
                }

                val items = response.results.onEach { it.selectedType = if (isTV) "tv" else "movie" }
                if (items.isEmpty()) {
                    isCategoryEnd = true
                } else {
                    _discoverItems.value = _discoverItems.value + items
                    currentDiscoverPage++
                }
            } catch (e: Exception) {
                // handle error
            } finally {
                _isDiscoverLoading.value = false
            }
        }
    }

    private fun performSearch(q: String) {
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val movieResp = apiService.searchMovies(q, 1)
                val tvResp = apiService.searchTVShows(q, 1)

                val movies = movieResp.results.onEach { it.selectedType = "movie" }
                val tvs = tvResp.results.onEach { it.selectedType = "tv" }

                val combined = (movies + tvs).sortedByDescending { it.popularity ?: 0.0 }
                _searchResults.value = combined
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun loadMediaDetails(item: MediaItem) {
        _isLoadingDetail.value = true
        viewModelScope.launch {
            try {
                val detail = if (item.itemType == "tv") {
                    apiService.getTVShowDetails(item.id)
                } else {
                    apiService.getMovieDetails(item.id)
                }
                _selectedMediaDetail.value = detail
            } catch (e: Exception) {
                // error loading details
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }
}
