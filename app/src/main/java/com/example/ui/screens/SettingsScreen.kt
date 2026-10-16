package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.data.model.EarthquakeNotificationPreference
import com.example.data.model.UserLocation
import com.example.ui.theme.*
import com.example.util.IndonesianCities

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notificationPref: EarthquakeNotificationPreference,
    userLocation: UserLocation,
    onUpdateNotificationPref: (EarthquakeNotificationPreference) -> Unit,
    onUpdateUserLocation: (UserLocation) -> Unit,
    onSendTestNotification: () -> Unit,
    onRequestGpsLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCityDialog by remember { mutableStateOf(false) }

    // Android 13+ Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin notifikasi ditolak. Peringatan gempa mungkin tidak muncul di layar kunci.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & Notifikasi", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // NOTIFICATION SETTINGS CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "NOTIFIKASI GEMPA BUMI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pemberitahuan darurat ke HP saat gempa terdeteksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationPref.enabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onUpdateNotificationPref(notificationPref.copy(enabled = isChecked))
                            },
                            modifier = Modifier.testTag("notification_master_switch")
                        )
                    }

                    if (notificationPref.enabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Minimum Magnitude
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Ambang Magnitudo Minimum",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "≥ M ${notificationPref.minMagnitude}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = notificationPref.minMagnitude,
                                onValueChange = { onUpdateNotificationPref(notificationPref.copy(minMagnitude = it)) },
                                valueRange = 3.0f..7.0f,
                                steps = 7
                            )
                            Text(
                                text = "Hanya kirim notifikasi jika kekuatan gempa melebihi ambang batas ini.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Radius Filter
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Radius Jangkauan Pantau",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (notificationPref.notifyAllRegions) "Semua Wilayah" else "${notificationPref.radiusKm.toInt()} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = notificationPref.notifyAllRegions,
                                    onCheckedChange = { onUpdateNotificationPref(notificationPref.copy(notifyAllRegions = it)) }
                                )
                                Text(text = "Pantau seluruh Indonesia & dunia", style = MaterialTheme.typography.bodySmall)
                            }
                            if (!notificationPref.notifyAllRegions) {
                                Slider(
                                    value = notificationPref.radiusKm,
                                    onValueChange = { onUpdateNotificationPref(notificationPref.copy(radiusKm = it)) },
                                    valueRange = 100f..1500f,
                                    steps = 13
                                )
                            }
                        }

                        // Specific Filters
                        ToggleOptionRow(
                            title = "Hanya Peringatan Berpotensi Tsunami",
                            subtitle = "Prioritaskan gempa yang mengeluarkan peringatan resmi tsunami BMKG",
                            checked = notificationPref.onlyTsunamiAlerts,
                            onCheckedChange = { onUpdateNotificationPref(notificationPref.copy(onlyTsunamiAlerts = it)) }
                        )

                        ToggleOptionRow(
                            title = "Hanya Gempa yang Dirasakan",
                            subtitle = "Kirim notifikasi hanya jika gempa terkonfirmasi dirasakan warga (MMI)",
                            checked = notificationPref.onlyFeltEarthquakes,
                            onCheckedChange = { onUpdateNotificationPref(notificationPref.copy(onlyFeltEarthquakes = it)) }
                        )

                        ToggleOptionRow(
                            title = "Suara & Getaran Darurat",
                            subtitle = "Gunakan nada dering dan pola getaran kuat untuk notifikasi kritis",
                            checked = notificationPref.soundEnabled && notificationPref.vibrateEnabled,
                            onCheckedChange = {
                                onUpdateNotificationPref(notificationPref.copy(soundEnabled = it, vibrateEnabled = it))
                            }
                        )

                        ToggleOptionRow(
                            title = "Jam Tenang (Malam Hari)",
                            subtitle = "Nonaktifkan getaran dan suara gempa ringan antara 22:00 - 06:00",
                            checked = notificationPref.quietHoursEnabled,
                            onCheckedChange = { onUpdateNotificationPref(notificationPref.copy(quietHoursEnabled = it)) }
                        )

                        // TEST NOTIFICATION BUTTON
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onSendTestNotification()
                                Toast.makeText(context, "Notifikasi uji coba terkirim! Periksa bar notifikasi HP Anda.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_notification_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim Uji Coba Notifikasi Sekarang")
                        }
                    }
                }
            }

            // LOCATION SETTINGS CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "LOKASI PANTAU PENGGUNA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Digunakan untuk menghitung jarak geodesik dan arah kompas ke pusat gempa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = userLocation.locationName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${userLocation.latitude}, ${userLocation.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showCityDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Ubah")
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onRequestGpsLocation,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gunakan Lokasi GPS Perangkat Ini")
                    }
                }
            }

            // OFFICIAL DATA SOURCES & ATTRIBUTION CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SUMBER DATA & KEBIJAKAN PRIVASI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    DataSourceItem(
                        name = "BMKG (Badan Meteorologi, Klimatologi, dan Geofisika)",
                        url = "https://data.bmkg.go.id/gempabumi/",
                        role = "Sumber Utama Nasional: AutoGempa, Gempa Terkini M 5.0+, dan Gempa Dirasakan (TEWS)"
                    )

                    DataSourceItem(
                        name = "USGS (United States Geological Survey)",
                        url = "https://earthquake.usgs.gov/earthquakes/feed/",
                        role = "Sumber Global: Pemantauan gempa bumi internasional real-time GeoJSON"
                    )

                    Text(
                        text = "Privasi: Koordinat lokasi pengguna hanya digunakan secara lokal di perangkat untuk menghitung jarak ke episentrum dan tidak pernah dikirim ke server pihak ketiga.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // City Selection Dialog
    if (showCityDialog) {
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = { Text("Pilih Kota Pantau", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    IndonesianCities.CITIES.forEach { city ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (city.locationName == userLocation.locationName) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateUserLocation(city)
                                    showCityDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = city.locationName,
                                modifier = Modifier.padding(10.dp),
                                fontWeight = if (city.locationName == userLocation.locationName) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun ToggleOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DataSourceItem(name: String, url: String, role: String) {
    Column {
        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text = role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = url, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
    }
}
