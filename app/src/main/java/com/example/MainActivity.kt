package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UnifiedEarthquake
import com.example.ui.EarthquakeViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.location.LocationServices

enum class MainDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    MAP("Peta", Icons.Filled.Map, Icons.Outlined.Map),
    HISTORY("Riwayat", Icons.Filled.History, Icons.Outlined.History),
    SAFETY("Siaga", Icons.Filled.HealthAndSafety, Icons.Outlined.HealthAndSafety),
    SETTINGS("Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: EarthquakeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Intent extra from Notification click
        intent?.getStringExtra("EXTRA_EARTHQUAKE_ID")?.let { id ->
            viewModel.selectEarthquakeById(id)
        }

        setContent {
            MyApplicationTheme {
                MainContent(
                    viewModel = viewModel,
                    onRequestGps = { checkAndRequestGps() }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("EXTRA_EARTHQUAKE_ID")?.let { id ->
            viewModel.selectEarthquakeById(id)
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkAndRequestGps() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateGpsLocation(location.latitude, location.longitude)
                }
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                101
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkAndRequestGps()
        }
    }
}

@Composable
fun MainContent(
    viewModel: EarthquakeViewModel,
    onRequestGps: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by remember { mutableStateOf(MainDestination.DASHBOARD) }
    var activeDetailEarthquake by remember { mutableStateOf<UnifiedEarthquake?>(null) }

    // If an earthquake was selected via notification or user click
    val selectedFromState = uiState.selectedEarthquake

    if (activeDetailEarthquake != null) {
        DetailScreen(
            earthquake = activeDetailEarthquake!!,
            isFavorite = uiState.favoriteIds.contains(activeDetailEarthquake!!.id),
            userLocation = uiState.userLocation,
            onBack = { activeDetailEarthquake = null },
            onToggleFavorite = { viewModel.toggleFavorite(activeDetailEarthquake!!.id) }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    MainDestination.values().forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(destination.title) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDestination) {
                    MainDestination.DASHBOARD -> {
                        DashboardScreen(
                            state = uiState,
                            filteredList = viewModel.getFilteredEarthquakes(),
                            onSelectEarthquake = { activeDetailEarthquake = it },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onFilterChange = { viewModel.setFilter(it) },
                            onRefresh = { viewModel.refreshData() },
                            onNavigateToSafety = { currentDestination = MainDestination.SAFETY }
                        )
                    }
                    MainDestination.MAP -> {
                        MapScreen(
                            state = uiState,
                            filteredEarthquakes = viewModel.getFilteredEarthquakes(),
                            selectedEarthquake = selectedFromState,
                            onSelectEarthquake = { viewModel.selectEarthquake(it) },
                            onNavigateToDetail = { activeDetailEarthquake = it },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onFilterChange = { viewModel.setFilter(it) }
                        )
                    }
                    MainDestination.HISTORY -> {
                        HistoryScreen(
                            state = uiState,
                            earthquakes = viewModel.getFilteredEarthquakes(),
                            onSelectEarthquake = { activeDetailEarthquake = it },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onMinMagChange = { viewModel.setMinMagFilter(it) },
                            onExportCsv = { viewModel.exportEarthquakesAsCsv() },
                            onExportJson = { viewModel.exportEarthquakesAsJson() }
                        )
                    }
                    MainDestination.SAFETY -> {
                        SafetyCenterScreen(
                            checklistItems = uiState.checklistItems,
                            onToggleChecklistItem = { id, isChecked -> viewModel.toggleChecklistItem(id, isChecked) }
                        )
                    }
                    MainDestination.SETTINGS -> {
                        SettingsScreen(
                            notificationPref = uiState.notificationPref,
                            userLocation = uiState.userLocation,
                            onUpdateNotificationPref = { viewModel.updateNotificationPref(it) },
                            onUpdateUserLocation = { viewModel.setUserLocation(it) },
                            onSendTestNotification = { viewModel.sendTestNotification() },
                            onRequestGpsLocation = onRequestGps
                        )
                    }
                }
            }
        }
    }
}
