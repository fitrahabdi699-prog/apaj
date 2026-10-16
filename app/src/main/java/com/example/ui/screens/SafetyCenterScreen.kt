package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ChecklistItemEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCenterScreen(
    checklistItems: List<ChecklistItemEntity>,
    onToggleChecklistItem: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedGuideTab by remember { mutableIntStateOf(0) } // 0: Tas Siaga, 1: Tsunami, 2: Saat Gempa, 3: Skala MMI

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pusat Kesiapsiagaan & Darurat", fontWeight = FontWeight.Bold) },
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
            // Hero Banner Image
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.safety_hero_banner),
                        contentDescription = "Tas Siaga Bencana dan Kesiapsiagaan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Emergency Contacts Hotline Quick Dial
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOMOR PANGGILAN DARURAT RESMI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AlertRed
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HotlineButton("112", "Darurat Umum", context, Modifier.weight(1f))
                        HotlineButton("115", "Basarnas (SAR)", context, Modifier.weight(1f))
                        HotlineButton("119", "Ambulans/Medis", context, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HotlineButton("110", "Polisi", context, Modifier.weight(1f))
                        HotlineButton("113", "Damkar", context, Modifier.weight(1f))
                        HotlineButton("196", "Call BMKG", context, Modifier.weight(1f))
                    }
                }
            }

            // Segment Tabs for Knowledge Base
            PrimaryTabRow(
                selectedTabIndex = selectedGuideTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedGuideTab == 0,
                    onClick = { selectedGuideTab = 0 },
                    text = { Text("Tas Siaga (TSB)", maxLines = 1) }
                )
                Tab(
                    selected = selectedGuideTab == 1,
                    onClick = { selectedGuideTab = 1 },
                    text = { Text("Evakuasi Tsunami", maxLines = 1) }
                )
                Tab(
                    selected = selectedGuideTab == 2,
                    onClick = { selectedGuideTab = 2 },
                    text = { Text("Panduan Gempa", maxLines = 1) }
                )
                Tab(
                    selected = selectedGuideTab == 3,
                    onClick = { selectedGuideTab = 3 },
                    text = { Text("Edukasi MMI", maxLines = 1) }
                )
            }

            // Tab Content
            when (selectedGuideTab) {
                0 -> TasSiagaChecklistSection(checklistItems, onToggleChecklistItem)
                1 -> TsunamiEvacuationSection()
                2 -> EarthquakeActionGuideSection()
                3 -> MmiScaleEducationSection()
            }
        }
    }
}

@Composable
private fun HotlineButton(number: String, label: String, context: Context, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(intent)
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = number, fontWeight = FontWeight.Black, fontSize = 16.sp, color = AlertRed)
            Text(text = label, fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun TasSiagaChecklistSection(
    items: List<ChecklistItemEntity>,
    onToggle: (String, Boolean) -> Unit
) {
    val completedCount = items.count { it.isChecked }
    val progress = if (items.isNotEmpty()) completedCount.toFloat() / items.size else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CHECKLIST TAS SIAGA BENCANA (TSB)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Persiapkan perlengkapan untuk bertahan hidup minimal 72 jam pertama pasca bencana.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Kesiapan: $completedCount dari ${items.size} item",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 1f) SafeTeal else WarningOrange
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (progress >= 1f) SafeTeal else WarningOrange
            )

            Spacer(modifier = Modifier.height(14.dp))

            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggle(item.id, !item.isChecked) }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { onToggle(item.id, it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun TsunamiEvacuationSection() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Golden time rule
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AlertRed.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = AlertRed)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PRINSIP EMAS (GOLDEN TIME) 15-20 MENIT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = AlertRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Jika Anda berada di daerah pesisir pantai dan merasakan guncangan gempa yang kuat ATAU berlangsung lama (>20 detik), JANGAN MENUNGGU SIRENE ATAU NOTIFIKASI APLIKASI! Segera evakuasi diri menjauhi pantai menuju tempat yang tinggi (minimal 20 meter di atas permukaan laut atau lantai 3+ gedung evakuasi tsunami).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = "TINGKATAN PERINGATAN RESMI BMKG",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TsunamiLevelCard("AWAS", "Perkiraan tinggi tsunami ≥ 3 Meter", "Pemerintah Daerah/BPBD harus segera mengarahkan evakuasi menyeluruh seluruh warga di pesisir ke dataran tinggi.", AlertRed)
            TsunamiLevelCard("SIAGA", "Perkiraan tinggi tsunami 0.5 - 3 Meter", "Evakuasi warga di sepanjang garis pantai dan sempadan muara sungai ke tempat yang aman.", WarningOrange)
            TsunamiLevelCard("WASPADA", "Perkiraan tinggi tsunami < 0.5 Meter", "Warga dilarang beraktivitas di pantai dan tepian sungai. Perahu nelayan segera sandar.", WarningYellow)
            TsunamiLevelCard("STATUS BERAKHIR", "Ancaman tsunami telah berlalu", "Hanya berlaku jika BMKG dan otoritas BPBD resmi mengumumkan status berakhir.", SafeTeal)
        }
    }
}

@Composable
private fun TsunamiLevelCard(level: String, sub: String, desc: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = level, fontWeight = FontWeight.Black, color = color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "• $sub", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EarthquakeActionGuideSection() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "PANDUAN KESELAMATAN SEBELUM, SAAT, & SETELAH GEMPA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            StepGuide(
                step = "1. SEBELUM GEMPA (MITIGASI)",
                items = listOf(
                    "Kenali jalur evakuasi dan titik kumpul aman di rumah atau kantor.",
                    "Kencangkan lemari dan perabot berat ke dinding agar tidak roboh.",
                    "Ketahui letak katup gas elpiji dan saklar utama pemutus listrik (MCB).",
                    "Simpan nomor darurat dan siapkan Tas Siaga Bencana (TSB)."
                )
            )

            StepGuide(
                step = "2. SAAT GEMPA TERJADI (DROP, COVER, HOLD ON)",
                items = listOf(
                    "Di dalam ruangan: Merunduk (Drop), Lindungi kepala di bawah meja kokoh (Cover), dan Bertahan pegang kaki meja (Hold On).",
                    "Jauhi jendela kaca, cermin, lampu gantung, dan dinding bata rapuh.",
                    "JANGAN gunakan lift! Jika berada di dalam lift, tekan semua tombol lantai dan segera keluar.",
                    "Di luar ruangan: Jauhi gedung tinggi, tiang listrik, papan reklame, dan pohon besar.",
                    "Di dalam mobil: Kurangi kecepatan perlahan, menepi di tempat terbuka, jangan berhenti di atas jembatan."
                )
            )

            StepGuide(
                step = "3. SETELAH GEMPA BERHENTI",
                items = listOf(
                    "Keluar gedung secara tertib menggunakan tangga darurat sambil melindungi kepala.",
                    "Periksa kebocoran gas: JANGAN menyalakan korek api atau saklar listrik jika mencium bau gas.",
                    "Waspadai gempa susulan (aftershock) yang berpotensi merobohkan struktur bangunan yang sudah retak.",
                    "Ikuti arahan resmi otoritas BMKG/BNPB melalui radio atau aplikasi GEMPAKU."
                )
            )
        }
    }
}

@Composable
private fun StepGuide(step: String, items: List<String>) {
    Column {
        Text(text = step, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))
        items.forEach { item ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(text = "•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MmiScaleEducationSection() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "EDUKASI SKALA INTENSITAS GEMPA (MMI)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Magnitudo mengukur total energi gempa di sumbernya (hiposentrum). Sedangkan MMI (Modified Mercalli Intensity) mengukur efek guncangan nyata yang dirasakan manusia dan tanah pada lokasi tertentu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MmiItem("I MMI", "Tidak dirasakan, hanya tercatat oleh alat seismograf peka.")
            MmiItem("II MMI", "Dirasakan oleh beberapa orang diam, terutama di lantai atas gedung.")
            MmiItem("III MMI", "Dirasakan nyata dalam rumah seakan-akan ada truk melintas.")
            MmiItem("IV MMI", "Piring pecah berdering, pintu/jendela berderik, dinding berbunyi.")
            MmiItem("V MMI", "Dirasakan hampir semua orang; perabot bergetar, barang pecah.")
            MmiItem("VI MMI", "Orang berhamburan keluar; plester dinding retak, cerobong rusak ringan.")
            MmiItem("VII MMI", "Sulit berdiri; kerusakan ringan pada bangunan dengan konstruksi kuat.")
            MmiItem("VIII MMI+", "Kerusakan parah pada bangunan biasa, dinding runtuh, longsor lereng.")
        }
    }
}

@Composable
private fun MmiItem(scale: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.width(64.dp)
        ) {
            Text(
                text = scale,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = desc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}
