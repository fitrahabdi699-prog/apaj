package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UnifiedEarthquake
import com.example.ui.EarthquakeUiState
import com.example.ui.components.EarthquakeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: EarthquakeUiState,
    earthquakes: List<UnifiedEarthquake>,
    onSelectEarthquake: (UnifiedEarthquake) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMinMagChange: (Float) -> Unit,
    onExportCsv: () -> String,
    onExportJson: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Semua, 1: Favorit
    var sortByMag by remember { mutableStateOf(false) }

    val displayList = remember(earthquakes, selectedTab, sortByMag, state.favoriteIds) {
        val base = if (selectedTab == 1) {
            earthquakes.filter { state.favoriteIds.contains(it.id) }
        } else earthquakes

        if (sortByMag) {
            base.sortedByDescending { it.magnitude }
        } else {
            base.sortedByDescending { it.timeEpochMillis }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pencarian & Riwayat Gempa", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val csv = onExportCsv()
                        shareExportFile(context, csv, "text/csv", "gempaku_catalog.csv")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Ekspor CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search TextField
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                placeholder = { Text("Cari kota, wilayah, pulau, atau ID gempa...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tabs: Semua vs Favorit
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Katalog (${earthquakes.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Favorit (${state.favoriteIds.size})") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter controls: Min Magnitude slider & Sort toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state.minMagFilter > 0) "Min: M ${state.minMagFilter.toInt()}" else "Semua M",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Slider(
                        value = state.minMagFilter,
                        onValueChange = onMinMagChange,
                        valueRange = 0f..7f,
                        steps = 6,
                        modifier = Modifier.width(130.dp)
                    )
                }

                FilterChip(
                    selected = sortByMag,
                    onClick = { sortByMag = !sortByMag },
                    label = { Text(if (sortByMag) "Urut: Magnitudo" else "Urut: Waktu") },
                    leadingIcon = {
                        Icon(if (sortByMag) Icons.Default.TrendingUp else Icons.Default.Schedule, contentDescription = null)
                    }
                )
            }

            // Quick Export buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val csv = onExportCsv()
                        shareExportFile(context, csv, "text/csv", "gempaku_catalog.csv")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor CSV")
                }

                OutlinedButton(
                    onClick = {
                        val json = onExportJson()
                        shareExportFile(context, json, "application/json", "gempaku_catalog.json")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor JSON")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Results List
            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 1) "Belum ada gempa yang disimpan ke favorit." else "Tidak ada gempa yang cocok dengan pencarian.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(displayList, key = { it.id }) { eq ->
                        EarthquakeCard(
                            earthquake = eq,
                            isFavorite = state.favoriteIds.contains(eq.id),
                            onCardClick = { onSelectEarthquake(eq) },
                            onFavoriteToggle = { onToggleFavorite(eq.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun shareExportFile(context: Context, data: String, mimeType: String, filename: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Ekspor Data Gempa GEMPAKU")
        putExtra(Intent.EXTRA_TEXT, data)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan / Simpan Data"))
}
