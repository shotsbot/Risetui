package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Analisis Pribadi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ringkasan Penggunaan", style = MaterialTheme.typography.titleLarge)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Iklan Diblokir",
                    numericValue = 1240,
                    formatValue = { String.format(Locale.US, "%,d", it) },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Data Dihemat",
                    numericValue = 340,
                    formatValue = { "$it MB" },
                    icon = { Icon(Icons.Default.DataUsage, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) }
                )
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Ancaman Dicegah",
                    numericValue = 12,
                    formatValue = { it.toString() },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Waktu Akses",
                    numericValue = 14,
                    formatValue = { "$it Jam" },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Performa Optimal", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Browser menggunakan GeckoView yang secara otomatis menghemat energi baterai layaknya mode hemat daya bawaan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bar Chart Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ancaman yang Diblokir (7 Hari Terakhir)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    SimpleBarChart(
                        data = listOf(
                            "Sen" to 14f,
                            "Sel" to 22f,
                            "Rab" to 10f,
                            "Kam" to 35f,
                            "Jum" to 18f,
                            "Sab" to 4f,
                            "Min" to 8f
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(data: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { it.second }.coerceAtLeast(1f)
    
    var animationPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, (label, value) ->
            val fraction = value / maxVal
            
            val animatedFraction by animateFloatAsState(
                targetValue = if (animationPlayed) fraction.coerceAtLeast(0.05f) else 0f,
                animationSpec = tween(
                    durationMillis = 1000,
                    delayMillis = index * 100,
                    easing = FastOutSlowInEasing
                ),
                label = "BarChartAnimation"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                if (animatedFraction > 0.05f) {
                    Text(
                        text = value.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(animatedFraction)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AnalyticsCard(modifier: Modifier = Modifier, title: String, numericValue: Int, formatValue: (Int) -> String, icon: @Composable () -> Unit) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedValue by animateIntAsState(
        targetValue = if (animationPlayed) numericValue else 0,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "AnalyticsAnimation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            icon()
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatValue(animatedValue), style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
