package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EarthquakeSource
import com.example.data.model.UnifiedEarthquake
import com.example.ui.ConnectionStatus
import com.example.ui.EarthquakeUiState
import com.example.ui.FeedFilter
import com.example.ui.components.EarthquakeCard
import com.example.ui.components.getMagnitudeColor
import com.example.ui.theme.*
import com.example.util.DateTimeUtils
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: EarthquakeUiState,
    filteredList: List<UnifiedEarthquake>,
    onSelectEarthquake: (UnifiedEarthquake) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onFilterChange: (FeedFilter) -> Unit,
    onRefresh: () -> Unit,
    onNavigateToSafety: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CoralRed,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEMPAKU",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    // Connection Status Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (state.connectionStatus) {
                            ConnectionStatus.ONLINE_LIVE -> SafeTeal.copy(alpha = 0.15f)
                            ConnectionStatus.OFFLINE_CACHED -> WarningOrange.copy(alpha = 0.15f)
                            ConnectionStatus.SYNCING -> InfoBlue.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (state.connectionStatus) {
                                            ConnectionStatus.ONLINE_LIVE -> SafeTeal
                                            ConnectionStatus.OFFLINE_CACHED -> WarningOrange
                                            ConnectionStatus.SYNCING -> InfoBlue
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when (state.connectionStatus) {
                                    ConnectionStatus.ONLINE_LIVE -> "LIVE"
                                    ConnectionStatus.OFFLINE_CACHED -> "OFFLINE"
                                    ConnectionStatus.SYNCING -> "SYNC..."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (state.connectionStatus) {
                                    ConnectionStatus.ONLINE_LIVE -> SafeTeal
                                    ConnectionStatus.OFFLINE_CACHED -> WarningOrange
                                    ConnectionStatus.SYNCING -> InfoBlue
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Perbarui Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error banner if any
            if (state.errorMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertRed.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AlertRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRed,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRefresh) {
                                Text("Coba Lagi", color = AlertRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // User Monitored Location bar
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (state.userLocation.isGps) Icons.Default.GpsFixed else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Titik Pantau: ${state.userLocation.locationName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Latest Earthquake Hero Card (BMKG AutoGempa)
            if (state.autoGempa != null) {
                item {
                    Text(
                        text = "GEMPA TERBARU (AUTOGEMPA BMKG)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LatestEarthquakeHeroCard(
                        earthquake = state.autoGempa,
                        isFavorite = state.favoriteIds.contains(state.autoGempa.id),
                        onClick = { onSelectEarthquake(state.autoGempa) },
                        onFavoriteToggle = { onToggleFavorite(state.autoGempa.id) }
                    )
                }
            }

            // Quick Stats 24H Summary Bar
            item {
                StatsSummarySection(earthquakes = state.earthquakes)
            }

            // Filter Chips Bar
            item {
                Column {
                    Text(
                        text = "DAFTAR GEMPA TERKINI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FeedFilter.values()) { filter ->
                            FilterChip(
                                selected = state.selectedFilter == filter,
                                onClick = { onFilterChange(filter) },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Earthquake List
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.isLoading) "Memuat data gempa..." else "Tidak ada gempa pada kategori filter ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { eq ->
                    EarthquakeCard(
                        earthquake = eq,
                        isFavorite = state.favoriteIds.contains(eq.id),
                        onCardClick = { onSelectEarthquake(eq) },
                        onFavoriteToggle = { onToggleFavorite(eq.id) }
                    )
                }
            }

            // Attribution & Official Source Notice
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Sumber Data Resmi & Atribusi:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• BMKG (Badan Meteorologi, Klimatologi, dan Geofisika) — Pusat Gempa Bumi dan Tsunami Indonesia\n• USGS (U.S. Geological Survey) Earthquake Hazards Program — Pemantauan Global",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LatestEarthquakeHeroCard(
    earthquake: UnifiedEarthquake,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val magColor = getMagnitudeColor(earthquake.magnitude)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("latest_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large magnitude badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(magColor, magColor.copy(alpha = 0.8f))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "M ${earthquake.magnitude}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp
                            )
                            Text(
                                text = earthquake.magnitudeType,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = DateTimeUtils.formatRelativeTime(earthquake.timeEpochMillis),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = earthquake.formattedTimeWib,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Simpan Favorit",
                        tint = if (isFavorite) WarningYellow else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Epicenter Place Name
            Text(
                text = earthquake.place,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Parameters Grid (Kedalaman, Koordinat, Jarak)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroParam(label = "Kedalaman", value = "${earthquake.depthKm.toInt()} km (${earthquake.depthCategory.label.split(" ")[0]})")
                HeroParam(label = "Koordinat", value = earthquake.rawCoordinatesString.take(15))
                if (earthquake.distanceKmFromUser != null) {
                    HeroParam(
                        label = "Jarak dari Anda",
                        value = "${GeoUtils.formatDistance(earthquake.distanceKmFromUser)} ${earthquake.compassDirectionFromUser ?: ""}"
                    )
                }
            }

            // Tsunami status banner
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (earthquake.tsunamiStatus.isWarning) AlertRed.copy(alpha = 0.15f) else SafeTeal.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (earthquake.tsunamiStatus.isWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (earthquake.tsunamiStatus.isWarning) AlertRed else SafeTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = earthquake.tsunamiDescription,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (earthquake.tsunamiStatus.isWarning) AlertRed else SafeTeal
                    )
                }
            }

            // BMKG Shakemap preview if available
            if (!earthquake.shakemapUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Peta Guncangan (Shakemap BMKG)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ketuk untuk melihat →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = earthquake.shakemapUrl,
                        contentDescription = "Peta Shakemap Gempa BMKG",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroParam(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatsSummarySection(earthquakes: List<UnifiedEarthquake>) {
    val totalCount = earthquakes.size
    val significantCount = earthquakes.count { it.magnitude >= 5.0 }
    val feltCount = earthquakes.count { !it.feltInfo.isNullOrBlank() }
    val maxMag = earthquakes.maxOfOrNull { it.magnitude } ?: 0.0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "Total Gempa", value = "$totalCount", color = MaterialTheme.colorScheme.onSurface)
            StatItem(label = "M ≥ 5.0", value = "$significantCount", color = WarningOrange)
            StatItem(label = "Dirasakan", value = "$feltCount", color = InfoBlue)
            StatItem(label = "Terbesar", value = if (maxMag > 0) "M $maxMag" else "-", color = AlertRed)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
