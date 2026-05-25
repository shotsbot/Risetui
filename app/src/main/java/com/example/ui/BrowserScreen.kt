package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.viewmodel.BrowserViewModel
import org.mozilla.geckoview.GeckoView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToTabs: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val isIncognito by viewModel.isIncognito.collectAsStateWithLifecycle()
    val isDesktopMode by viewModel.isDesktopMode.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val activeSession = viewModel.activeSession

    val bgColor = if (isIncognito) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.background
    val topBarColor = if (isIncognito) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface

    val snackbarHostState = remember { SnackbarHostState() }
    
    var showTranslateDialog by remember { mutableStateOf(false) }
    var languageDetected by remember { mutableStateOf("Mendeteksi...") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topBarColor)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                        var inputText by remember(urlInput) { mutableStateOf(urlInput) }
                        var isFocused by remember { mutableStateOf(false) }
                        
                        val searchBarWidth by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (isFocused) 1.05f else 1f,
                            label = "searchBarWidth"
                        )
                        
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .scale(searchBarWidth)
                                .onFocusChanged { state -> isFocused = state.isFocused }
                                .clip(RoundedCornerShape(28.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isIncognito) Color(0xFF3C3C3C) else MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = if (isIncognito) Color(0xFF3C3C3C) else MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            viewModel.loadUrl(inputText)
                        }),
                        leadingIcon = {
                            if (isIncognito) {
                                Icon(Icons.Default.Security, contentDescription = "Incognito", tint = textColor)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = textColor)
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.reload() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = textColor)
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = topBarColor,
                contentColor = textColor,
                tonalElevation = 8.dp
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = { viewModel.goForward() }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(onClick = { onNavigateToDashboard() }) {
                        Icon(Icons.Default.Analytics, contentDescription = "Dashboard")
                    }
                    IconButton(onClick = { onNavigateToTabs() }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FilterNone, contentDescription = "Tabs")
                            Text(
                                text = tabs.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    var showBookmarkDialog by remember { mutableStateOf(false) }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Buka Riwayat") },
                                onClick = {
                                    onNavigateToHistory()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isDesktopMode) "Matikan Mode Desktop" else "Nyalakan Mode Desktop") },
                                onClick = {
                                    viewModel.toggleDesktopMode()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isIncognito) "Tutup Mode Penyamaran" else "Mode Penyamaran") },
                                onClick = {
                                    viewModel.toggleIncognito()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Buka Bookmark") },
                                onClick = {
                                    onNavigateToBookmarks()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pengaturan & Privasi") },
                                onClick = {
                                    onNavigateToSettings()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tambahkan ke Bookmark") },
                                onClick = {
                                    showBookmarkDialog = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("IDM Downloads") },
                                onClick = {
                                    onNavigateToDownloads()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Download Hal. Ini (IDM)") },
                                onClick = {
                                    viewModel.downloadCurrentPage()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tambahkan ke Layar Utama") },
                                onClick = {
                                    viewModel.createDesktopShortcut(context)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Terjemahkan Halaman") },
                                onClick = {
                                    showTranslateDialog = true
                                    showMenu = false
                                    languageDetected = "Mendeteksi..."
                                    coroutineScope.launch {
                                        delay(1000)
                                        languageDetected = "Selesai (Otomatis)"
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simpan Luring (PDF)") },
                                onClick = {
                                    viewModel.savePageOffline(context, asPdf = true)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simpan Luring (HTML)") },
                                onClick = {
                                    viewModel.savePageOffline(context, asPdf = false)
                                    showMenu = false
                                }
                            )
                        }
                    }

                    if (showBookmarkDialog) {
                        var bookmarkTitle by remember { mutableStateOf("Bookmark") }
                        var bookmarkTag by remember { mutableStateOf("") }
                        val predefinedTags = listOf("Work", "Personal", "Research", "News")
                        
                        AlertDialog(
                            onDismissRequest = { showBookmarkDialog = false },
                            title = { Text("Simpan Bookmark") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = bookmarkTitle,
                                        onValueChange = { bookmarkTitle = it },
                                        label = { Text("Judul") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )
                                    OutlinedTextField(
                                        value = bookmarkTag,
                                        onValueChange = { bookmarkTag = it },
                                        label = { Text("Kategori / Tag") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )
                                    Text("Saran Kategori:", style = MaterialTheme.typography.labelMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        predefinedTags.forEach { tag ->
                                            InputChip(
                                                selected = bookmarkTag == tag,
                                                onClick = { bookmarkTag = tag },
                                                label = { Text(tag) }
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.saveBookmark(bookmarkTitle, urlInput, bookmarkTag)
                                    showBookmarkDialog = false
                                }) {
                                    Text("Simpan")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBookmarkDialog = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }

                    if (showTranslateDialog) {
                        AlertDialog(
                            onDismissRequest = { showTranslateDialog = false },
                            title = { Text("Terjemahkan Halaman") },
                            text = { Text("Bahasa terdeteksi: $languageDetected\nUbah ke: Bahasa Indonesia") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showTranslateDialog = false
                                        if (urlInput.isNotEmpty()) {
                                            val encodedUrl = java.net.URLEncoder.encode(urlInput, "UTF-8")
                                            viewModel.loadUrl("https://translate.google.com/translate?sl=auto&tl=id&u=$encodedUrl")
                                        }
                                    },
                                    enabled = languageDetected != "Mendeteksi..."
                                ) {
                                    Text("Terjemahkan")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTranslateDialog = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            AndroidView(
                factory = { ctx ->
                    GeckoView(ctx).apply {
                        activeSession?.let { setSession(it) }
                    }
                },
                update = { view ->
                    activeSession?.let { view.setSession(it) }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
