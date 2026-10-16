package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EarthquakeSource
import com.example.data.model.UnifiedEarthquake
import com.example.data.model.UserLocation
import com.example.ui.components.InteractiveEpicenterMap
import com.example.ui.components.getMagnitudeColor
import com.example.ui.theme.*
import com.example.util.DateTimeUtils
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    earthquake: UnifiedEarthquake,
    isFavorite: Boolean,
    userLocation: UserLocation,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val magColor = getMagnitudeColor(earthquake.magnitude)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Gempa Bumi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorit",
                            tint = if (isFavorite) WarningYellow else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { shareEarthquake(context, earthquake) }) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan Informasi Gempa")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Large Magnitude Banner
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(magColor, magColor.copy(alpha = 0.8f))))
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "M ${earthquake.magnitude}",
                                    color = Color.White,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = earthquake.magnitudeType,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        // Source pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = earthquake.source.displayName,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = earthquake.place,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Waktu WIB: ${earthquake.formattedTimeWib}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Waktu UTC: ${earthquake.formattedTimeUtc}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tsunami Warning Section
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (earthquake.tsunamiStatus.isWarning) AlertRed.copy(alpha = 0.12f) else SafeTeal.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (earthquake.tsunamiStatus.isWarning) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (earthquake.tsunamiStatus.isWarning) AlertRed else SafeTeal,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "STATUS POTENSI TSUNAMI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (earthquake.tsunamiStatus.isWarning) AlertRed else SafeTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = earthquake.tsunamiDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (earthquake.tsunamiStatus.isWarning)
                                "Segera ikuti instruksi evakuasi otoritas BNPB/BPBD. Jauhi pantai dan tepian sungai menuju tempat tinggi!"
                            else
                                "Berdasarkan pemodelan resmi saat ini, tidak ada ancaman tsunami. Namun bila Anda berada di pesisir dan merasakan guncangan kuat/lama, jangan tunggu sirene: segera evakuasi ke tempat tinggi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Distance & Epicenter Parameters
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "PARAMETER EPISENTRUM & HIPOSENTRUM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ParameterRow(
                        label = "Kedalaman (Hiposentrum)",
                        value = "${earthquake.depthKm} km",
                        subtitle = earthquake.depthCategory.label
                    )

                    ParameterRow(
                        label = "Koordinat Episentrum",
                        value = "${earthquake.latitude}°, ${earthquake.longitude}°",
                        subtitle = earthquake.rawCoordinatesString
                    )

                    if (earthquake.distanceKmFromUser != null) {
                        ParameterRow(
                            label = "Perkiraan Jarak dari Lokasi Anda",
                            value = "${GeoUtils.formatDistance(earthquake.distanceKmFromUser)} arah ${earthquake.compassDirectionFromUser ?: ""}",
                            subtitle = "Dihitung dari: ${userLocation.locationName}"
                        )
                    }

                    ParameterRow(
                        label = "Status Data",
                        value = if (earthquake.eventStatus == "confirmed") "Terkonfirmasi Otoritas" else "Otomatis (Automatic Feed)",
                        subtitle = "Beda magnitudo (energi) vs intensitas MMI (guncangan setempat)"
                    )
                }
            }

            // Felt Intensity (MMI) section if available
            if (!earthquake.feltInfo.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "WILAYAH DIRASAKAN & SKALA MMI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = InfoBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = earthquake.feltInfo,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Skala MMI mengukur dampak guncangan yang dirasakan manusia dan kerusakan bangunan di lokasi spesifik.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Mini Map showing Epicenter
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PETA EPISENTRUM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InteractiveEpicenterMap(
                        earthquakes = listOf(earthquake),
                        selectedEarthquake = earthquake,
                        userLocation = userLocation,
                        onSelectEarthquake = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }

            // Shakemap Image (BMKG)
            if (!earthquake.shakemapUrl.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PETA GUNCANGAN RESMI BMKG (SHAKEMAP)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = earthquake.shakemapUrl,
                                contentDescription = "Peta Guncangan Shakemap BMKG",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // External Links & Official Source Button
            if (!earthquake.officialUrl.isNullOrBlank()) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(earthquake.officialUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Laman Resmi ${if (earthquake.source == EarthquakeSource.BMKG) "BMKG" else "USGS"}")
                }
            }
        }
    }
}

@Composable
private fun ParameterRow(label: String, value: String, subtitle: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun shareEarthquake(context: Context, earthquake: UnifiedEarthquake) {
    val shareText = buildString {
        append("🚨 INFO GEMPA BUMI — GEMPAKU\n\n")
        append("• Magnitudo: M ${earthquake.magnitude} (${earthquake.magnitudeType})\n")
        append("• Wilayah: ${earthquake.place}\n")
        append("• Kedalaman: ${earthquake.depthKm} km\n")
        append("• Waktu: ${earthquake.formattedTimeWib}\n")
        append("• Koordinat: ${earthquake.latitude}, ${earthquake.longitude}\n")
        if (earthquake.distanceKmFromUser != null) {
            append("• Perkiraan Jarak: ${GeoUtils.formatDistance(earthquake.distanceKmFromUser!!)}")
            if (!earthquake.compassDirectionFromUser.isNullOrBlank()) {
                append(" arah ${earthquake.compassDirectionFromUser}\n")
            } else {
                append("\n")
            }
        }
        append("• Status Tsunami: ${earthquake.tsunamiDescription}\n")
        if (!earthquake.feltInfo.isNullOrBlank()) {
            append("• Dirasakan: ${earthquake.feltInfo}\n")
        }
        append("• Sumber Resmi: ${earthquake.source.displayName}\n\n")
        append("Dipantau melalui aplikasi GEMPAKU.")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Informasi Gempa"))
}
