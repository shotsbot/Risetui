package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.BrowserViewModel

import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class SearchEngineOption(val name: String, val url: String)
val SEARCH_ENGINES = listOf(
    SearchEngineOption("Google", "https://www.google.com/search?q="),
    SearchEngineOption("DuckDuckGo", "https://duckduckgo.com/?q="),
    SearchEngineOption("Ecosia", "https://www.ecosia.org/search?q="),
    SearchEngineOption("Bing", "https://www.bing.com/search?q=")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: BrowserViewModel, onBack: () -> Unit, onNavigateToExtensions: () -> Unit) {
    val adBlockerEnabled by viewModel.adBlockerEnabled.collectAsStateWithLifecycle()
    val searchEngineUrl by viewModel.searchEngine.collectAsStateWithLifecycle()
    val clearDataOnExit by viewModel.clearDataOnExit.collectAsStateWithLifecycle()
    val syncFrequency by viewModel.syncFrequency.collectAsStateWithLifecycle()
    
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    
    var showSearchEngineMenu by remember { mutableStateOf(false) }
    var showSyncFrequencyMenu by remember { mutableStateOf(false) }
    val currentSearchEngineName = SEARCH_ENGINES.find { it.url == searchEngineUrl }?.name ?: "Kustom"
    
    val syncFrequencies = listOf("Mati", "Setiap Jam", "Setiap 6 Jam", "Harian")
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoaded = true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & Privasi") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Privasi & Keamanan", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            
            AnimatedVisibility(visible = isLoaded, enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleAdBlocker() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pemblokir Iklan & Pelacak", style = MaterialTheme.typography.bodyLarge)
                            Text("Blokir konten berbahaya secara otomatis", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = adBlockerEnabled, onCheckedChange = { viewModel.toggleAdBlocker() })
                    }
                }
            }
            
            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 100)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 100))) {
                ListItem(
                    headlineContent = { Text("Mode Gelap") },
                    supportingContent = { Text("Tema aplikasi gelap atau terang") },
                    leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.toggleDarkMode() }
                )
            }

            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 150)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 150))) {
                ListItem(
                    headlineContent = { Text("Hapus Data Saat Keluar") },
                    supportingContent = { Text("Hapus cookie dan data sementara saat aplikasi ditutup") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = clearDataOnExit,
                            onCheckedChange = { viewModel.toggleClearDataOnExit() }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.toggleClearDataOnExit() }
                )
            }

            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 200)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 200))) {
                ListItem(
                    headlineContent = { Text("Hapus Data Browsing") },
                    supportingContent = { Text("Membersihkan semua riwayat dan data situs") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.clearBrowsingData() }
                )
            }

            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 250)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 250))) {
                Box {
                    ListItem(
                        headlineContent = { Text("Mesin Pencari") },
                        supportingContent = { Text(currentSearchEngineName) },
                        leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSearchEngineMenu = true }
                    )
                    
                    DropdownMenu(
                        expanded = showSearchEngineMenu,
                        onDismissRequest = { showSearchEngineMenu = false }
                    ) {
                        SEARCH_ENGINES.forEach { engine ->
                            DropdownMenuItem(
                                text = { Text(engine.name) },
                                onClick = {
                                    viewModel.setSearchEngine(engine.url)
                                    showSearchEngineMenu = false
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 250)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 250))) {
                ListItem(
                    headlineContent = { Text("Ekstensi (Add-ons)") },
                    supportingContent = { Text("Kelola ekstensi pihak ketiga yang diinstal") },
                    leadingContent = { Icon(Icons.Default.Extension, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToExtensions() }
                )
            }

            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 300)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 300))) {
                Box {
                    ListItem(
                        headlineContent = { Text("Frekuensi Sinkronisasi Cloud") },
                        supportingContent = { Text(syncFrequency) },
                        leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSyncFrequencyMenu = true }
                    )
                    
                    DropdownMenu(
                        expanded = showSyncFrequencyMenu,
                        onDismissRequest = { showSyncFrequencyMenu = false }
                    ) {
                        syncFrequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    viewModel.setSyncFrequency(freq)
                                    showSyncFrequencyMenu = false
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 350)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 350))) {
                ListItem(
                    headlineContent = { Text("Ekspor Data") },
                    supportingContent = { Text("Unduh data pribadi untuk pencadangan offline") },
                    leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {}
                )
            }

            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 400)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 400))) {
                ListItem(
                    headlineContent = { Text("Bahasa & Wilayah") },
                    supportingContent = { Text("Multibahasa Didukung: Indonesia") },
                    leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {}
                )
            }
            
            AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(delayMillis = 450)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(delayMillis = 450))) {
                ListItem(
                    headlineContent = { Text("Otentikasi Dua Faktor (2FA)") },
                    supportingContent = { Text("Tingkatkan keamanan akses sinkronisasi akun") },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {}
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
