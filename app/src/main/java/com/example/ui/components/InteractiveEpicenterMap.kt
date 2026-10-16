package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UnifiedEarthquake
import com.example.data.model.UserLocation
import com.example.ui.theme.*
import kotlin.math.hypot

@Composable
fun InteractiveEpicenterMap(
    earthquakes: List<UnifiedEarthquake>,
    selectedEarthquake: UnifiedEarthquake?,
    userLocation: UserLocation?,
    onSelectEarthquake: (UnifiedEarthquake) -> Unit,
    modifier: Modifier = Modifier
) {
    // Geo bounds for initial view (Indonesia region: Longitude 94 to 142, Latitude -11 to 6)
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val minLon = 94.0
    val maxLon = 142.0
    val minLat = -11.5
    val maxLat = 6.5

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B1626))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(earthquakes, scale, offsetX, offsetY) {
                    detectTapGestures { tapOffset ->
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()

                        // Find closest earthquake to tap
                        var closest: UnifiedEarthquake? = null
                        var minDistance = 50f // touch target threshold in px

                        earthquakes.forEach { eq ->
                            // Project lat/lon to screen
                            val normX = (eq.longitude - minLon) / (maxLon - minLon)
                            val normY = (maxLat - eq.latitude) / (maxLat - minLat)

                            val screenX = ((normX.toFloat() * width) * scale) + offsetX + (width * (1f - scale) / 2f)
                            val screenY = ((normY.toFloat() * height) * scale) + offsetY + (height * (1f - scale) / 2f)

                            val d = hypot(tapOffset.x - screenX, tapOffset.y - screenY)
                            if (d < minDistance) {
                                minDistance = d
                                closest = eq
                            }
                        }

                        closest?.let { onSelectEarthquake(it) }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            fun projectToScreen(lat: Double, lon: Double): Offset {
                val normX = (lon - minLon) / (maxLon - minLon)
                val normY = (maxLat - lat) / (maxLat - minLat)
                val screenX = ((normX.toFloat() * width) * scale) + offsetX + (width * (1f - scale) / 2f)
                val screenY = ((normY.toFloat() * height) * scale) + offsetY + (height * (1f - scale) / 2f)
                return Offset(screenX, screenY)
            }

            // Draw coordinate grid lines
            val gridColor = Color(0xFF1E3A5F).copy(alpha = 0.5f)
            val strokeGrid = Stroke(width = 1f)

            // Equator (Lat 0)
            val equatorStart = projectToScreen(0.0, minLon)
            val equatorEnd = projectToScreen(0.0, maxLon)
            drawLine(
                color = Color(0xFFE63946).copy(alpha = 0.4f),
                start = equatorStart,
                end = equatorEnd,
                strokeWidth = 1.5f
            )

            // Approximate stylized regional coastlines (Sumatra, Java, Kalimantan, Sulawesi, Papua)
            val islandColor = Color(0xFF192E4D)
            val islandBorder = Color(0xFF2E4E7E)

            // Sumatra outline curve
            val sumatra = Path().apply {
                val p1 = projectToScreen(5.5, 95.3)
                moveTo(p1.x, p1.y)
                val p2 = projectToScreen(3.0, 97.0)
                lineTo(p2.x, p2.y)
                val p3 = projectToScreen(-0.5, 100.5)
                lineTo(p3.x, p3.y)
                val p4 = projectToScreen(-3.5, 103.5)
                lineTo(p4.x, p4.y)
                val p5 = projectToScreen(-5.8, 105.8)
                lineTo(p5.x, p5.y)
                val p6 = projectToScreen(-4.5, 105.0)
                lineTo(p6.x, p6.y)
                val p7 = projectToScreen(-1.0, 101.0)
                lineTo(p7.x, p7.y)
                close()
            }
            drawPath(sumatra, color = islandColor)
            drawPath(sumatra, color = islandBorder, style = Stroke(1.5f))

            // Java outline
            val java = Path().apply {
                val p1 = projectToScreen(-6.0, 106.0)
                moveTo(p1.x, p1.y)
                val p2 = projectToScreen(-6.8, 110.0)
                lineTo(p2.x, p2.y)
                val p3 = projectToScreen(-7.2, 114.2)
                lineTo(p3.x, p3.y)
                val p4 = projectToScreen(-8.5, 114.0)
                lineTo(p4.x, p4.y)
                val p5 = projectToScreen(-7.8, 109.0)
                lineTo(p5.x, p5.y)
                val p6 = projectToScreen(-7.0, 106.0)
                lineTo(p6.x, p6.y)
                close()
            }
            drawPath(java, color = islandColor)
            drawPath(java, color = islandBorder, style = Stroke(1.5f))

            // Kalimantan
            val kalimantan = Path().apply {
                val p1 = projectToScreen(4.0, 115.0)
                moveTo(p1.x, p1.y)
                val p2 = projectToScreen(1.0, 118.0)
                lineTo(p2.x, p2.y)
                val p3 = projectToScreen(-3.5, 116.0)
                lineTo(p3.x, p3.y)
                val p4 = projectToScreen(-3.0, 111.0)
                lineTo(p4.x, p4.y)
                val p5 = projectToScreen(1.5, 109.5)
                lineTo(p5.x, p5.y)
                close()
            }
            drawPath(kalimantan, color = islandColor)
            drawPath(kalimantan, color = islandBorder, style = Stroke(1.5f))

            // Sulawesi
            val sulawesi = Path().apply {
                val p1 = projectToScreen(1.5, 124.5)
                moveTo(p1.x, p1.y)
                val p2 = projectToScreen(0.5, 120.0)
                lineTo(p2.x, p2.y)
                val p3 = projectToScreen(-5.0, 119.5)
                lineTo(p3.x, p3.y)
                val p4 = projectToScreen(-4.0, 122.5)
                lineTo(p4.x, p4.y)
                val p5 = projectToScreen(-1.0, 121.0)
                lineTo(p5.x, p5.y)
                close()
            }
            drawPath(sulawesi, color = islandColor)
            drawPath(sulawesi, color = islandBorder, style = Stroke(1.5f))

            // Papua
            val papua = Path().apply {
                val p1 = projectToScreen(-0.8, 131.0)
                moveTo(p1.x, p1.y)
                val p2 = projectToScreen(-2.5, 140.5)
                lineTo(p2.x, p2.y)
                val p3 = projectToScreen(-8.5, 140.5)
                lineTo(p3.x, p3.y)
                val p4 = projectToScreen(-4.5, 135.0)
                lineTo(p4.x, p4.y)
                close()
            }
            drawPath(papua, color = islandColor)
            drawPath(papua, color = islandBorder, style = Stroke(1.5f))

            // Draw line from user location to selected earthquake if available
            if (userLocation != null && selectedEarthquake != null) {
                val userPt = projectToScreen(userLocation.latitude, userLocation.longitude)
                val eqPt = projectToScreen(selectedEarthquake.latitude, selectedEarthquake.longitude)
                drawLine(
                    color = SafeTeal.copy(alpha = 0.8f),
                    start = userPt,
                    end = eqPt,
                    strokeWidth = 2.5f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )
            }

            // Draw Earthquake Epicenters
            earthquakes.forEach { eq ->
                val pt = projectToScreen(eq.latitude, eq.longitude)
                val isSelected = selectedEarthquake?.id == eq.id
                val color = when {
                    eq.magnitude >= 6.0 -> AlertRed
                    eq.magnitude >= 5.0 -> WarningOrange
                    eq.magnitude >= 4.0 -> WarningYellow
                    else -> SafeTeal
                }

                val radius = (eq.magnitude.toFloat() * 3.5f * scale.coerceAtLeast(0.8f)).coerceIn(8f, 40f)

                // Outer glow ring for M5+ or Selected
                if (eq.magnitude >= 5.0 || isSelected) {
                    drawCircle(
                        color = color.copy(alpha = if (isSelected) 0.45f else 0.2f),
                        radius = radius * (if (isSelected) 1.8f else 1.4f),
                        center = pt
                    )
                }

                // Core circle
                drawCircle(
                    color = color,
                    radius = radius,
                    center = pt
                )

                // White center pip
                drawCircle(
                    color = Color.White,
                    radius = (radius * 0.35f).coerceAtLeast(2f),
                    center = pt
                )

                // Selected border highlight
                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = radius + 3f,
                        center = pt,
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // Draw User Location Pin
            if (userLocation != null) {
                val userPt = projectToScreen(userLocation.latitude, userLocation.longitude)
                // Pulse halo
                drawCircle(
                    color = Color(0xFF00B4D8).copy(alpha = 0.25f),
                    radius = 22f * scale.coerceAtLeast(0.8f),
                    center = userPt
                )
                drawCircle(
                    color = Color(0xFF0077B6),
                    radius = 9f,
                    center = userPt
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = userPt
                )
            }
        }

        // Overlay Map Controls (Zoom in, Zoom out, Reset)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = { scale = (scale * 1.3f).coerceAtMost(5f) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = NavySurface.copy(alpha = 0.9f)),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Perbesar", tint = Color.White)
            }
            FilledIconButton(
                onClick = { scale = (scale / 1.3f).coerceAtLeast(0.5f) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = NavySurface.copy(alpha = 0.9f)),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Perkecil", tint = Color.White)
            }
            FilledIconButton(
                onClick = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = NavySurface.copy(alpha = 0.9f)),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = "Pusatkan Indonesia", tint = Color.White)
            }
        }

        // Map Legend at Top Left
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = NavyBackground.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(AlertRed, "≥6.0")
                LegendItem(WarningOrange, "5.0-5.9")
                LegendItem(WarningYellow, "4.0-4.9")
                LegendItem(SafeTeal, "<4.0")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}
