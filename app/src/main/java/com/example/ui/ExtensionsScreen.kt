package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Extension
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(viewModel: BrowserViewModel, onBack: () -> Unit) {
    val extensions by viewModel.extensions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDiscoverSheet by remember { mutableStateOf(false) }
    var selectedToInstall by remember { mutableStateOf<Extension?>(null) }
    var selectedExtensionDetails by remember { mutableStateOf<Extension?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ekstensi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDiscoverSheet = true },
                icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                text = { Text("Temukan Ekstensi") }
            )
        }
    ) { innerPadding ->
        if (extensions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ExtensionOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Belum ada ekstensi yang diinstal.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(extensions) { ext ->
                    ExtensionCard(
                        extension = ext,
                        onToggle = { isEnabled -> viewModel.toggleExtension(ext.id, isEnabled) },
                        onUninstall = { viewModel.uninstallExtension(ext.id) },
                        onClick = { selectedExtensionDetails = ext }
                    )
                }
            }
        }
    }
    
    if (showDiscoverSheet) {
        ModalBottomSheet(onDismissRequest = { showDiscoverSheet = false }) {
            DiscoverExtensionsView(
                onInstallRequest = { 
                    selectedToInstall = it
                    showDiscoverSheet = false
                },
                onDetailRequest = {
                    selectedExtensionDetails = it
                }
            )
        }
    }
    
    selectedToInstall?.let { ext ->
        AlertDialog(
            onDismissRequest = { selectedToInstall = null },
            title = { Text("Konfirmasi Instalasi") },
            text = {
                Column {
                    Text("Apakah Anda ingin menginstal ${ext.name}?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ekstensi ini memerlukan izin berikut:", fontWeight = FontWeight.Bold)
                    Text(ext.permissions, style = MaterialTheme.typography.bodySmall)
                    if (!ext.isVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Peringatan Keamanan: Ekstensi ini berasal dari pengembang pihak ketiga dan belum diverifikasi secara resmi. Instal dengan risiko Anda sendiri.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.installExtension(ext)
                    selectedToInstall = null
                }) {
                    Text("Setuju & Instal")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedToInstall = null }) {
                    Text("Batal")
                }
            }
        )
    }
    
    selectedExtensionDetails?.let { ext ->
        val isInstalled = extensions.any { it.id == ext.id }
        ExtensionDetailDialog(
            extension = ext,
            isInstalled = isInstalled,
            onDismiss = { selectedExtensionDetails = null },
            onInstall = { selectedToInstall = ext },
            onUninstall = { viewModel.uninstallExtension(ext.id) }
        )
    }
}

@Composable
fun ExtensionCard(
    extension: Extension,
    onToggle: (Boolean) -> Unit,
    onUninstall: () -> Unit,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Extension, 
                        contentDescription = null, 
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(extension.name, style = MaterialTheme.typography.titleMedium)
                    Text("Versi ${extension.version} • oleh ${extension.author}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = extension.isEnabled,
                    onCheckedChange = onToggle
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(extension.description, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onUninstall, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Copot Pemasangan")
                }
            }
        }
    }
}

@Composable
fun DiscoverExtensionsView(
    onInstallRequest: (Extension) -> Unit,
    onDetailRequest: (Extension) -> Unit
) {
    val mockExtensions = listOf(
        Extension("ext_adblock", "AdGuard Ultimate", "Pemblokir iklan tingkat lanjut dengan filter kustom.", "1.2.0", "AdGuard Team", "", true, "Akses ke seluruh data situs web", "https://example.com/ext/adblock", true),
        Extension("ext_darkreader", "Dark Reader", "Tema gelap untuk semua situs web.", "4.9.40", "Alexander Bykov", "", true, "Membaca dan mengubah data Anda di semua situs web", "https://example.com/ext/darkreader", true),
        Extension("ext_grease", "Tampermonkey", "Manajer skrip web paling populer.", "4.12", "Jan Biniok", "", true, "Menjalankan skrip, memodifikasi halaman web", "https://example.com/ext/tampermonkey", true),
        Extension("ext_crypto", "Wallet Connect", "Dompet crypto untuk Web3.", "1.0.1", "Web3 Dev", "", true, "Injeksi skrip, manajemen sesi", "https://example.com/ext/crypto", false)
    )
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredExtensions = remember(searchQuery) {
        mockExtensions.filter {
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.author.contains(searchQuery, ignoreCase = true)
        }
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Temukan Ekstensi", style = MaterialTheme.typography.headlineSmall)
        Text("Ditingkatkan untuk GeckoView", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari ekstensi atau pembuat...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filteredExtensions) { ext ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onDetailRequest(ext) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ext.name, style = MaterialTheme.typography.titleMedium)
                            if (ext.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Terverifikasi", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(ext.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Button(onClick = { onInstallRequest(ext) }) {
                        Text("Pasang")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionDetailDialog(
    extension: Extension,
    isInstalled: Boolean,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Detail Ekstensi") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.Default.Extension, 
                                    contentDescription = null, 
                                    modifier = Modifier.padding(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(extension.name, style = MaterialTheme.typography.titleLarge)
                                    if (extension.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, contentDescription = "Terverifikasi", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Text("Versi ${extension.version} • oleh ${extension.author}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    
                    item {
                        if (isInstalled) {
                            Button(onClick = { 
                                onUninstall()
                                onDismiss() 
                            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("Copot Pemasangan")
                            }
                        } else {
                            Button(onClick = { 
                                onInstall()
                                onDismiss() 
                            }) {
                                Text("Pasang Ekstensi")
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Ringkasan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(extension.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Deskripsi Lengkap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ekstensi ini membawa fungsionalitas yang kuat langsung ke browser Anda. Nikmati pengalaman berselancar yang lebih personal, aman, dan dapat disesuaikan. Dibuat dengan standar kualitas terbaru untuk memastikan performa tinggi tanpa memengaruhi kecepatan akses Anda secara keseluruhan.", style = MaterialTheme.typography.bodyMedium)
                    }

                    item {
                        Text("Cuplikan Layar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(3) { index ->
                                Surface(
                                    modifier = Modifier.size(160.dp, 240.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.padding(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("Ulasan Pengguna", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.StarHalf, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("4.5 / 5.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Sangat membantu dan mempercepat produktivitas. Salah satu ekstensi terbaik yang pernah saya coba di platform ini.", style = MaterialTheme.typography.bodySmall)
                                Text("- Pengguna Anonim", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    item {
                        Text("Izin Diperlukan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(extension.permissions, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(32.dp)) // Bottom padding
                    }
                }
            }
        }
    }
}
