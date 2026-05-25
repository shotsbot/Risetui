package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.BrowserViewModel
import com.example.utils.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: BrowserViewModel, onBack: () -> Unit) {
    val downloader = viewModel.downloader
    val tasks by downloader?.tasks?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: mutableStateOf(emptyList())
    var newDownloadUrl by remember { mutableStateOf("") }
    
    val speedLimit by viewModel.downloadSpeedLimit.collectAsStateWithLifecycle()
    var showSpeedMenu by remember { mutableStateOf(false) }

    val speedOptions = listOf(
        0 to "Tanpa Batas",
        100 to "Lambat (100 KB/s)",
        500 to "Sedang (500 KB/s)",
        2048 to "Cepat (2 MB/s)",
        5120 to "Sangat Cepat (5 MB/s)"
    )
    val currentSpeedLabel = speedOptions.find { it.first == speedLimit }?.second ?: "Kustom (${speedLimit} KB/s)"
    
    var showExitConfirmation by remember { mutableStateOf(false) }
    val hasActiveDownloads = tasks.any { it.status == DownloadStatus.DOWNLOADING }
    
    val handleBack = {
        if (hasActiveDownloads) {
            showExitConfirmation = true
        } else {
            onBack()
        }
    }

    androidx.activity.compose.BackHandler(enabled = true, onBack = handleBack)

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Konfirmasi Keluar") },
            text = { Text("Terdapat proses download yang sedang berjalan. Apakah Anda yakin ingin keluar dari halaman ini?") },
            confirmButton = {
                Button(onClick = {
                    showExitConfirmation = false
                    onBack()
                }) {
                    Text("Ya, Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IDM Downloads") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Add new download section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newDownloadUrl,
                    onValueChange = { newDownloadUrl = it },
                    placeholder = { Text("Masukkan URL untuk di-download") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newDownloadUrl.isNotEmpty()) {
                            viewModel.startDownload(newDownloadUrl)
                            newDownloadUrl = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            }

            Box {
                ListItem(
                    headlineContent = { Text("Batasan Kecepatan Download") },
                    supportingContent = { Text(currentSpeedLabel) },
                    leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSpeedMenu = true }
                )
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false }
                ) {
                    speedOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.second) },
                            onClick = {
                                viewModel.setDownloadSpeedLimit(opt.first)
                                showSpeedMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Belum ada unduhan", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("File yang Anda unduh akan muncul di sini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tasks) { task ->
                        ListItem(
                            headlineContent = { Text(task.fileName) },
                            supportingContent = {
                                Column {
                                    Text("Status: ${task.status.name}")
                                    if (task.status == DownloadStatus.DOWNLOADING) {
                                        LinearProgressIndicator(
                                            progress = { task.progress },
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        )
                                        Text(
                                            "${task.downloadedBytes / 1024} KB / ${task.totalBytes / 1024} KB",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    } else if (task.status == DownloadStatus.COMPLETED) {
                                        Text(
                                            "Selesai (${task.totalBytes / 1024} KB)",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            },
                            leadingContent = {
                                Icon(Icons.Default.Download, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
