package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UnifiedEarthquake
import com.example.data.model.UserLocation
import com.example.ui.EarthquakeUiState
import com.example.ui.FeedFilter
import com.example.ui.components.EarthquakeCard
import com.example.ui.components.InteractiveEpicenterMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    state: EarthquakeUiState,
    filteredEarthquakes: List<UnifiedEarthquake>,
    selectedEarthquake: UnifiedEarthquake?,
    onSelectEarthquake: (UnifiedEarthquake) -> Unit,
    onNavigateToDetail: (UnifiedEarthquake) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onFilterChange: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Peta Episentrum Gempa", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Map Canvas filling the screen
            InteractiveEpicenterMap(
                earthquakes = filteredEarthquakes,
                selectedEarthquake = selectedEarthquake,
                userLocation = state.userLocation,
                onSelectEarthquake = onSelectEarthquake,
                modifier = Modifier.fillMaxSize()
            )

            // Top Filter Chips Overlay
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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

            // Bottom Selected Earthquake Preview Card
            if (selectedEarthquake != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    EarthquakeCard(
                        earthquake = selectedEarthquake,
                        isFavorite = state.favoriteIds.contains(selectedEarthquake.id),
                        onCardClick = { onNavigateToDetail(selectedEarthquake) },
                        onFavoriteToggle = { onToggleFavorite(selectedEarthquake.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
