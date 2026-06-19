package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.LocalConfiguration
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.MovieDetail
import com.example.model.TVSeason
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamVaultApp(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val selectedGenreId by viewModel.selectedGenreId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val trendingMovies by viewModel.trendingMovies.collectAsState()
    val trendingTVShows by viewModel.trendingTVShows.collectAsState()
    val nowPlayingMovies by viewModel.nowPlayingMovies.collectAsState()
    val onAirTVShows by viewModel.onAirTVShows.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()

    val discoverItems by viewModel.discoverItems.collectAsState()
    val isDiscoverLoading by viewModel.isDiscoverLoading.collectAsState()

    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val selectedDetail by viewModel.selectedMediaDetail.collectAsState()
    val isLoadingDetail by viewModel.isLoadingDetail.collectAsState()

    val playingMedia by viewModel.playingMedia.collectAsState()
    val playerSeason by viewModel.playerSeason.collectAsState()
    val playerEpisode by viewModel.playerEpisode.collectAsState()
    val playerProviderIdx by viewModel.playerProviderIndex.collectAsState()
    val playerUrl by viewModel.playerUrl.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Full screen overlays for HTML5 videos in AdBlockingWebView
    var html5FullscreenView by remember { mutableStateOf<View?>(null) }

    // Floating Search bar active on mobile
    var mobileSearchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StreamVaultBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StreamVaultBg.copy(alpha = 0.95f))
            ) {
                // Stacked Header Row 1: Logo & Search Toggle Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LogoComponent(onClick = {
                        viewModel.selectTab("Home")
                        viewModel.selectGenre(0)
                        mobileSearchExpanded = false
                    })

                    IconButton(
                        onClick = {
                            mobileSearchExpanded = !mobileSearchExpanded
                            if (mobileSearchExpanded) {
                                viewModel.selectTab("Movies")
                            } else {
                                viewModel.updateSearchQuery("")
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (mobileSearchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search icon",
                            tint = if (mobileSearchExpanded) StreamVaultAccent else StreamVaultSub
                        )
                    }
                }

                // Stacked Header Row 2: Capsule layout category tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Home", "Series", "Movies").forEach { tab ->
                        val isSelected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) StreamVaultAccent else StreamVaultBg3)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) StreamVaultAccent else StreamVaultBrd,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    viewModel.selectTab(tab)
                                    mobileSearchExpanded = false
                                }
                                .padding(vertical = 6.dp, horizontal = 14.dp)
                        ) {
                            Text(
                                text = if (tab == "Series") "TV Shows" else tab,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else StreamVaultSub
                            )
                        }
                    }
                }

                // Expanding search bar
                AnimatedVisibility(visible = mobileSearchExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search films, TV shows...", color = StreamVaultSub, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("search_text_input"),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = StreamVaultText,
                                unfocusedTextColor = StreamVaultText,
                                focusedContainerColor = StreamVaultBg3,
                                unfocusedContainerColor = StreamVaultBg3,
                                focusedBorderColor = StreamVaultAccent,
                                unfocusedBorderColor = StreamVaultBrd
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Filled.Clear, "Clear Text", tint = StreamVaultSub)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                        )
                    }
                }

                // Stacked Header Row 3: Genres scrolling layout
                GenreChipsRow(
                    selectedGenreId = selectedGenreId,
                    onGenreSelected = { genreId ->
                        viewModel.selectGenre(genreId)
                        if (genreId != 0 && activeTab == "Home") {
                            viewModel.selectTab("Movies")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (searchQuery.isNotBlank()) {
                // Search Results Grid Screen
                SearchResultsGrid(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    query = searchQuery,
                    onItemClick = { viewModel.selectMediaItem(it) }
                )
            } else if (activeTab == "Home") {
                // HOME SECTIONS SCREEN
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Hero Featured Box
                    item {
                        var heroItem: MediaItem? by remember { mutableStateOf(null) }
                        LaunchedEffect(trendingMovies) {
                            if (trendingMovies is UiState.Success) {
                                heroItem = (trendingMovies as UiState.Success<List<MediaItem>>).data.firstOrNull()
                            }
                        }

                        HeroFeaturedSlider(
                            heroItem = heroItem,
                            onWatchClick = { hero -> viewModel.startPlaying(hero) },
                            onInfoClick = { hero -> viewModel.selectMediaItem(hero) }
                        )
                    }

                    // Section 1: Trending Movies
                    item {
                        MediaListSection(
                            title = "Trending Movies",
                            accentTitle = "Movies",
                            state = trendingMovies,
                            onItemClick = { viewModel.selectMediaItem(it) },
                            onSeeAllClick = { viewModel.selectTab("Movies") }
                        )
                    }

                    // Section 2: Trending TV Shows
                    item {
                        MediaListSection(
                            title = "Trending TV Shows",
                            accentTitle = "TV Shows",
                            state = trendingTVShows,
                            onItemClick = { viewModel.selectMediaItem(it) },
                            onSeeAllClick = { viewModel.selectTab("Series") }
                        )
                    }

                    // Section 3: Now Playing
                    item {
                        MediaListSection(
                            title = "Now Playing",
                            accentTitle = "Playing",
                            state = nowPlayingMovies,
                            onItemClick = { viewModel.selectMediaItem(it) },
                            onSeeAllClick = { viewModel.selectTab("Movies") }
                        )
                    }

                    // Section 4: On Air Now
                    item {
                        MediaListSection(
                            title = "On Air Now",
                            accentTitle = "Air Now",
                            state = onAirTVShows,
                            onItemClick = { viewModel.selectMediaItem(it) },
                            onSeeAllClick = { viewModel.selectTab("Series") }
                        )
                    }

                    // Section 5: Top Rated
                    item {
                        MediaListSection(
                            title = "Top Rated",
                            accentTitle = "Rated",
                            state = topRatedMovies,
                            onItemClick = { viewModel.selectMediaItem(it) },
                            onSeeAllClick = { viewModel.selectTab("Movies") }
                        )
                    }

                    // In-app attribution footer info
                    item {
                        FooterAttributionView()
                    }
                }
            } else {
                // DISCOVER GRID VIEW (Movies or TV tab results)
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (activeTab == "Series") "📺 TV Series" else "🎬 Movies",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StreamVaultText,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    DiscoverGridScreen(
                        items = discoverItems,
                        isLoading = isDiscoverLoading,
                        onItemClick = { viewModel.selectMediaItem(it) },
                        onLoadMore = { viewModel.loadDiscoverData() }
                    )
                }

                // Initial discovery load
                LaunchedEffect(activeTab, selectedGenreId) {
                    if (discoverItems.isEmpty() && !isDiscoverLoading) {
                        viewModel.loadDiscoverData()
                    }
                }
            }

            // INFO MODAL (Detail Sheet Dialog)
            selectedMedia?.let { media ->
                DetailInfoDialog(
                    media = media,
                    detail = selectedDetail,
                    isLoading = isLoadingDetail,
                    onClose = { viewModel.selectMediaItem(null) },
                    onWatchNow = { item ->
                        viewModel.selectMediaItem(null)
                        viewModel.startPlaying(item)
                    }
                )
            }

            // PLAYER BOTTOM SHEET / SCREEN
            playingMedia?.let { media ->
                val consoleLogs by viewModel.consoleLogs.collectAsState()
                StreamPlayerDialog(
                    media = media,
                    providerIdx = playerProviderIdx,
                    season = playerSeason,
                    episode = playerEpisode,
                    playerUrl = playerUrl,
                    detail = selectedDetail,
                    consoleLogs = consoleLogs,
                    onConsoleMessage = { log -> viewModel.addConsoleLog(log) },
                    onClearLogs = { viewModel.clearConsoleLogs() },
                    onClose = { viewModel.stopPlaying() },
                    onProviderChange = { idx -> viewModel.setPlayerProvider(idx) },
                    onSeasonChange = { s, epCount -> viewModel.setPlayerSeason(s, epCount) },
                    onEpisodeChange = { ep -> viewModel.setPlayerEpisode(ep) },
                    onFullscreenShow = { view -> html5FullscreenView = view },
                    onFullscreenHide = { html5FullscreenView = null }
                )
            }

            // Fullscreen View Overlay for HTML5 video elements (i.e., browser full screen inside webview client!)
            html5FullscreenView?.let { customView ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable(enabled = false) {}
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            (customView.parent as? ViewGroup)?.removeView(customView)
                            FrameLayout(it).apply {
                                addView(customView, FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                ))
                            }
                        }
                    )
                }
                BackHandler {
                    html5FullscreenView = null
                }
            }
        }
    }
}

@Composable
fun LogoComponent(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Text(
            text = "STREAM",
            color = StreamVaultAccent,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.SansSerif
        )
        Text(
            text = "VAULT",
            color = StreamVaultGold,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun GenreChipsRow(
    selectedGenreId: Int,
    onGenreSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(GENRES_LIST) { genre ->
            val isSelected = selectedGenreId == genre.id
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) StreamVaultAccent else StreamVaultBg3)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) StreamVaultAccent else StreamVaultBrd,
                        shape = CircleShape
                    )
                    .clickable { onGenreSelected(genre.id) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = genre.name,
                    color = if (isSelected) Color.White else StreamVaultSub,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun HeroFeaturedSlider(
    heroItem: MediaItem?,
    onWatchClick: (MediaItem) -> Unit,
    onInfoClick: (MediaItem) -> Unit
) {
    if (heroItem == null) {
        // Shimmer skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(StreamVaultBg3)
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .background(Color.Black)
    ) {
        // Backdrop Image
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w780${heroItem.backdropPath ?: heroItem.posterPath}",
            contentDescription = "Featured movie backdrop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient over photo to create dark Cinematic focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            StreamVaultBg.copy(alpha = 0.3f),
                            StreamVaultBg
                        ),
                        startY = 100f
                    )
                )
        )

        // Text Content details aligned at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(StreamVaultAccent.copy(alpha = 0.15f))
                    .border(1.dp, StreamVaultAccent.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🔥 Trending #1",
                    color = StreamVaultAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = heroItem.displayTitle,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 36.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (heroItem.ratingString.isNotBlank()) {
                    Text(
                        text = "★ ${heroItem.ratingString}",
                        color = StreamVaultGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (heroItem.releaseYear.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(StreamVaultBg3, RoundedCornerShape(4.dp))
                            .border(1.dp, StreamVaultBrd, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = heroItem.releaseYear,
                            color = StreamVaultSub,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(StreamVaultBg3, RoundedCornerShape(4.dp))
                        .border(1.dp, StreamVaultBrd, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FILM",
                        color = StreamVaultSub,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = heroItem.overview ?: "",
                color = StreamVaultSub,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onWatchClick(heroItem) },
                    colors = ButtonDefaults.buttonColors(containerColor = StreamVaultAccent),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
                    modifier = Modifier.testTag("hero_watch_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Watch now",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { onInfoClick(heroItem) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "More details",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Info", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun MediaListSection(
    title: String,
    accentTitle: String,
    state: UiState<List<MediaItem>>,
    onItemClick: (MediaItem) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleParts = title.split(" $accentTitle")
            val baseTitle = titleParts.firstOrNull() ?: ""
            Row {
                Text(
                    text = baseTitle + " ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = accentTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StreamVaultAccent,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Text(
                text = "See All →",
                color = StreamVaultAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onSeeAllClick() }
                    .padding(4.dp)
            )
        }

        when (state) {
            is UiState.Loading -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(5) {
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(210.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StreamVaultBg3)
                        )
                    }
                }
            }
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.data) { item ->
                        MediaCard(item = item, onClick = { onItemClick(item) })
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unable to fetch content right now.",
                        color = StreamVaultSub,
                        fontSize = 12.sp,
                        textAlign = Alignment.Center.run { TextAlign.Center }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useFixedHorizontalWidth: Boolean = true
) {
    Card(
        modifier = modifier
            .then(if (useFixedHorizontalWidth) Modifier.width(130.dp) else Modifier.fillMaxWidth())
            .clickable { onClick() }
            .testTag("media_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StreamVaultCard),
        border = BorderStroke(1.dp, StreamVaultBrd)
    ) {
        Column {
            // Card Poster Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
                    .background(StreamVaultBg3)
            ) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w185${item.posterPath}",
                    contentDescription = item.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top Badge "FILM" in red or "SERIES" in violet
                val isTV = item.itemType == "tv"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isTV) Color(0xFF7C3AED) else StreamVaultAccent)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isTV) "SERIES" else "FILM",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Hover playing overlay look (subtle hover style)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 120f
                            )
                        )
                )
            }

            // Info Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(7.dp)
            ) {
                Text(
                    text = item.displayTitle,
                    color = StreamVaultText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.ratingString.isNotBlank()) {
                        Text(
                            text = "★ ${item.ratingString}",
                            color = StreamVaultGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(2.dp)
                                .clip(CircleShape)
                                .background(StreamVaultSub)
                        )
                    }

                    Text(
                        text = item.releaseYear,
                        color = StreamVaultSub,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultsGrid(
    searchResults: List<MediaItem>,
    isSearching: Boolean,
    query: String,
    onItemClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Text("Search ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StreamVaultText)
                Text("Results", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StreamVaultAccent)
            }
            Text("${searchResults.size} results", fontSize = 11.sp, color = StreamVaultSub)
        }

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StreamVaultAccent)
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No results for \"$query\"", color = StreamVaultSub, fontSize = 13.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(searchResults) { item ->
                    MediaCard(
                        item = item, 
                        onClick = { onItemClick(item) },
                        useFixedHorizontalWidth = false
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverGridScreen(
    items: List<MediaItem>,
    isLoading: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onLoadMore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        if (items.isEmpty() && isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StreamVaultAccent)
            }
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No items found inside this category", color = StreamVaultSub, fontSize = 13.sp)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items) { item ->
                        MediaCard(
                            item = item, 
                            onClick = { onItemClick(item) },
                            useFixedHorizontalWidth = false
                        )
                    }
                }

                if (!isLoading) {
                    Button(
                        onClick = onLoadMore,
                        colors = ButtonDefaults.buttonColors(containerColor = StreamVaultBg3),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StreamVaultBrd),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Text("Load More", color = StreamVaultText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StreamVaultAccent, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailInfoDialog(
    media: MediaItem,
    detail: MovieDetail?,
    isLoading: Boolean,
    onClose: () -> Unit,
    onWatchNow: (MediaItem) -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = StreamVaultBg2,
            border = BorderStroke(1.dp, StreamVaultBrd)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Backdrop with Close overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(StreamVaultBg3)
                ) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w780${media.backdropPath ?: media.posterPath}",
                        contentDescription = "Show Backdrop detail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, StreamVaultBg2),
                                    startY = 60f
                                )
                            )
                    )

                    // Close Button Floating
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(30.dp)
                    ) {
                        Icon(Icons.Filled.Close, "Close Detail Modal", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Info Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = media.displayTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Meta Tags row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (media.ratingString.isNotBlank()) {
                            Text(
                                text = "★ ${media.ratingString}",
                                color = StreamVaultGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (media.releaseYear.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(StreamVaultBg3, RoundedCornerShape(4.dp))
                                    .border(1.dp, StreamVaultBrd, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(media.releaseYear, color = StreamVaultSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(StreamVaultBg3, RoundedCornerShape(4.dp))
                                .border(1.dp, StreamVaultBrd, RoundedCornerShape(4.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (media.itemType == "tv") "TV SHOW" else "MOVIE",
                                color = StreamVaultSub,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (detail?.runtime != null) {
                            Box(
                                modifier = Modifier
                                    .background(StreamVaultBg3, RoundedCornerShape(4.dp))
                                    .border(1.dp, StreamVaultBrd, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text("${detail.runtime}m", color = StreamVaultSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (detail?.numberOfSeasons != null) {
                            Box(
                                modifier = Modifier
                                    .background(StreamVaultBg3, RoundedCornerShape(4.dp))
                                    .border(1.dp, StreamVaultBrd, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text("${detail.numberOfSeasons} Seasons", color = StreamVaultAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Overview
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = StreamVaultAccent, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Text(
                            text = detail?.overview ?: media.overview ?: "No synopsis available.",
                            fontSize = 12.sp,
                            color = StreamVaultSub,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Genres dynamic list
                    detail?.genres?.let { genreList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            genreList.take(4).forEach { g ->
                                Box(
                                    modifier = Modifier
                                        .background(StreamVaultBg3, RoundedCornerShape(4.dp))
                                        .border(1.dp, StreamVaultBrd, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(g.name, color = StreamVaultSub, fontSize = 10.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // Action Player Watch Now Button
                    Button(
                        onClick = { onWatchNow(media) },
                        colors = ButtonDefaults.buttonColors(containerColor = StreamVaultAccent),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("detail_dialog_watch_now"),
                        contentPadding = PaddingValues()
                    ) {
                        Icon(Icons.Filled.PlayArrow, "Arrow play controller icon", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamPlayerDialog(
    media: MediaItem,
    providerIdx: Int,
    season: Int,
    episode: Int,
    playerUrl: String,
    detail: MovieDetail?,
    consoleLogs: List<String>,
    onConsoleMessage: (String) -> Unit,
    onClearLogs: () -> Unit,
    onClose: () -> Unit,
    onProviderChange: (Int) -> Unit,
    onSeasonChange: (Int, Int) -> Unit,
    onEpisodeChange: (Int) -> Unit,
    onFullscreenShow: (View) -> Unit,
    onFullscreenHide: () -> Unit
) {
    val context = LocalContext.current
    var isWebLoading by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (isLandscape) {
                // Interactive widescreen landscape mode (Cinema View)
                Box(modifier = Modifier.fillMaxSize()) {
                    if (playerUrl.isNotBlank()) {
                        AdBlockingWebView(
                            url = playerUrl,
                            modifier = Modifier.fillMaxSize(),
                            onFullscreenShow = onFullscreenShow,
                            onFullscreenHide = onFullscreenHide,
                            onLoadingStateChanged = { isWebLoading = it },
                            onConsoleMessage = onConsoleMessage
                        )

                        if (isWebLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = StreamVaultAccent)
                            }
                        }
                    }

                    // Floating close button to exit landscape player cleanly
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Exit video stream",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Standard visual portrait controls with servers, selector lists & episodes
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top control bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StreamVaultBg2)
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, "Close Active Stream Player", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (media.itemType == "tv") "${media.displayTitle} S$season E$episode" else media.displayTitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Share button
                    IconButton(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Watching ${media.displayTitle} on StreamVault: $playerUrl")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Filled.Share, "Share Stream Controller", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Copy Link clipboard button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("StreamUrl", playerUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "✓ URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, "Copy Clipboard URL Controller", tint = Color.White, modifier = Modifier.size(15.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Live Web Console Log button
                    var showConsoleModal by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showConsoleModal = true },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Filled.BugReport, "Open Developer Web Console", tint = StreamVaultAccent, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Brave Custom Tab Launcher
                    IconButton(
                        onClick = {
                            if (playerUrl.isNotBlank()) {
                                try {
                                    val customTabsIntent = CustomTabsIntent.Builder().build()
                                    customTabsIntent.intent.setPackage("com.brave.browser")
                                    customTabsIntent.launchUrl(context, Uri.parse(playerUrl))
                                } catch (e: Exception) {
                                    // Fallback if Brave script fails or not installed properly for intent
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playerUrl))
                                    context.startActivity(intent)
                                }
                            }
                        },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Filled.Launch, "Open in Brave Browser", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    if (showConsoleModal) {
                        BrowserConsoleDialog(
                            logs = consoleLogs,
                            onClear = onClearLogs,
                            onDismiss = { showConsoleModal = false }
                        )
                    }
                }

                // Servers provider chips selector
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StreamVaultBg2)
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(PROVIDERS.size) { idx ->
                        val p = PROVIDERS[idx]
                        val isSel = providerIdx == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isSel) StreamVaultAccent.copy(alpha = 0.15f) else StreamVaultBg3)
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) StreamVaultAccent else StreamVaultBrd,
                                    shape = RoundedCornerShape(7.dp)
                                )
                                .clickable { onProviderChange(idx) }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${p.label} [${p.note}]",
                                color = if (isSel) StreamVaultAccent else StreamVaultText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Webview Player Canvas Area - Center player vertically & horizontally
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        if (playerUrl.isNotBlank()) {
                            AdBlockingWebView(
                                url = playerUrl,
                                modifier = Modifier.fillMaxSize(),
                                onFullscreenShow = onFullscreenShow,
                                onFullscreenHide = onFullscreenHide,
                                onLoadingStateChanged = { isWebLoading = it },
                                onConsoleMessage = onConsoleMessage
                            )

                            if (isWebLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = StreamVaultAccent)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Shields up! Loading safe stream...", color = StreamVaultSub, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Seasons / Episodes Selector Picker if it is a TV show
                if (media.itemType == "tv") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f)
                            .background(StreamVaultBg2)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Season Selector Heading
                        Text("SEASON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StreamVaultSub, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        val seasonsList = detail?.seasons?.filter { it.seasonNumber > 0 } ?: emptyList()
                        if (seasonsList.isEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Syncing seasons layout...", color = StreamVaultSub, fontSize = 11.sp)
                            }
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                seasonsList.forEach { s ->
                                    val isSelectedS = season == s.seasonNumber
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelectedS) StreamVaultAccent else StreamVaultBg3)
                                            .border(1.dp, if (isSelectedS) StreamVaultAccent else StreamVaultBrd, RoundedCornerShape(6.dp))
                                            .clickable { onSeasonChange(s.seasonNumber, s.episodeCount) }
                                            .padding(horizontal = 11.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = s.name ?: "S${s.seasonNumber}",
                                            color = if (isSelectedS) Color.White else StreamVaultSub,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Episode grid list
                        Text("EPISODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StreamVaultSub, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        val activeSeasonDetail = seasonsList.firstOrNull { it.seasonNumber == season }
                        val epCount = activeSeasonDetail?.episodeCount ?: 12

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            (1..epCount).forEach { epNum ->
                                val isSelectedE = episode == epNum
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelectedE) StreamVaultAccent else StreamVaultBg3)
                                        .border(1.dp, if (isSelectedE) StreamVaultAccent else StreamVaultBrd, RoundedCornerShape(6.dp))
                                        .clickable { onEpisodeChange(epNum) }
                                        .padding(horizontal = 11.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "E$epNum",
                                        color = if (isSelectedE) Color.White else StreamVaultText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } // Close original Column
        } // Close Landscape/Portrait else branch
    } // Close Surface
} // Close Dialog
} // Close StreamPlayerDialog Composable

@Composable
fun BrowserConsoleDialog(
    logs: List<String>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .border(1.dp, StreamVaultBrd, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = StreamVaultBg2
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.BugReport,
                            contentDescription = "Console Logs",
                            tint = StreamVaultAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Browser Developer Console",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Clear logs",
                                tint = StreamVaultSub,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Vault Console", logs.joinToString("\n"))
                                clipboard.setPrimaryClip(clip)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy all logs",
                                tint = StreamVaultSub,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = StreamVaultSub,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Console is clear.\nLoad movies or cycle servers to trace active streaming files.",
                                color = StreamVaultSub,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val state = androidx.compose.foundation.lazy.rememberLazyListState()
                        // Keep synced to bottom logs
                        LaunchedEffect(logs.size) {
                            if (logs.isNotEmpty()) {
                                state.scrollToItem(logs.size - 1)
                            }
                        }

                        LazyColumn(
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs) { log ->
                                val color = when {
                                    log.contains("[ERROR]") || log.contains("⚠️") || log.contains("fail") || log.contains("Fail") -> Color(0xFFFF6B6B)
                                    log.contains("🛡️") -> StreamVaultAccent
                                    log.contains("🎬") || log.contains("🚀") -> Color(0xFF4DABF7)
                                    else -> StreamVaultText
                                }
                                Text(
                                    text = log,
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FooterAttributionView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(color = StreamVaultBrd, thickness = 1.dp)
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "© 2026 StreamVault · TMDB data API · VidSrc/VidLink/2Embed streams\n" +
                    "All content is obtained via third-party web crawlers with in-app ad blocking\n" +
                    "Designed in Compose for StreamVault. All rights reserved.",
            color = StreamVaultSub,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            textAlign = Alignment.Center.run { TextAlign.Center }
        )
    }
}
