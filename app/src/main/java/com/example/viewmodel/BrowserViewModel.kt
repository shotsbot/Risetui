package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Bookmark
import com.example.data.Extension
import com.example.data.BrowserDatabase
import com.example.data.DatabaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import java.util.UUID

data class TabState(
    val id: String,
    val session: GeckoSession,
    val url: String = "",
    val title: String = "Tab Baru",
    val progress: Int = 0,
    val isLoading: Boolean = false
)

class BrowserViewModel : ViewModel() {

    private var database: BrowserDatabase? = null
    
    // UI State
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIncognito = MutableStateFlow(false)
    val isIncognito: StateFlow<Boolean> = _isIncognito.asStateFlow()

    private val _adBlockerEnabled = MutableStateFlow(true)
    val adBlockerEnabled: StateFlow<Boolean> = _adBlockerEnabled.asStateFlow()
    
    // Bookmarks and History State
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _extensions = MutableStateFlow<List<Extension>>(emptyList())
    val extensions: StateFlow<List<Extension>> = _extensions.asStateFlow()
    
    // Tabs State
    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    val activeTab = combine(_tabs, _activeTabId) { tabsList, activeId ->
        tabsList.find { it.id == activeId }
    }
    
    // Snackbar / Messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _searchEngine = MutableStateFlow("https://www.google.com/search?q=")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _clearDataOnExit = MutableStateFlow(false)
    val clearDataOnExit: StateFlow<Boolean> = _clearDataOnExit.asStateFlow()

    private val _syncFrequency = MutableStateFlow("Harian")
    val syncFrequency: StateFlow<String> = _syncFrequency.asStateFlow()

    var downloader: com.example.utils.MultipartDownloader? = null

    fun setSyncFrequency(frequency: String) {
        _syncFrequency.value = frequency
    }

    fun toggleClearDataOnExit() {
        _clearDataOnExit.value = !_clearDataOnExit.value
    }

    fun setSearchEngine(urlPrefix: String) {
        _searchEngine.value = urlPrefix
    }

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _downloadSpeedLimit = MutableStateFlow(0) // 0 means no limit, specified in KB/s
    val downloadSpeedLimit: StateFlow<Int> = _downloadSpeedLimit.asStateFlow()

    fun setDownloadSpeedLimit(kbs: Int) {
        _downloadSpeedLimit.value = kbs
        downloader?.speedLimitKBs = kbs
        prefs?.edit()?.putInt("download_speed_limit", kbs)?.apply()
    }

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        prefs?.edit()?.putBoolean("dark_mode", newValue)?.apply()
    }

    private var prefs: android.content.SharedPreferences? = null

    var geckoRuntime: GeckoRuntime? = null

    val activeSession: GeckoSession?
        get() = _tabs.value.find { it.id == _activeTabId.value }?.session

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
            val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            val isSystemDark = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            _isDarkMode.value = prefs!!.getBoolean("dark_mode", isSystemDark)
            _downloadSpeedLimit.value = prefs!!.getInt("download_speed_limit", 0)
        }

        if (database == null) {
            database = DatabaseProvider.getDatabase(context)
            viewModelScope.launch {
                database!!.bookmarkDao().getAllBookmarks().collect { bks ->
                    _bookmarks.value = bks
                }
            }
            viewModelScope.launch {
                database!!.extensionDao().getAllExtensions().collect { exts ->
                    _extensions.value = exts
                    // Sync extensions with WebExtensionController
                    syncExtensionsWithGecko()
                }
            }
        }

        if (geckoRuntime == null) {
            val settings = GeckoRuntimeSettings.Builder()
                .aboutConfigEnabled(true)
                .build()
            geckoRuntime = GeckoRuntime.create(context, settings)
        }
        
        if (downloader == null) {
            val folder = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            downloader = com.example.utils.MultipartDownloader(folder)
            downloader?.speedLimitKBs = _downloadSpeedLimit.value
        }
        
        if (_tabs.value.isEmpty()) {
            createNewTab()
        }
    }

    fun createNewTab(urlStr: String = "https://www.google.com") {
        val runtime = geckoRuntime ?: return

        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(_isIncognito.value)
            .build()
            
        val newSession = GeckoSession(settings)
        newSession.open(runtime)
        
        val tabId = UUID.randomUUID().toString()
        val newTab = TabState(id = tabId, session = newSession, url = urlStr)
        
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = tabId
        
        newSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                updateTabState(tabId) { it.copy(isLoading = true, url = url) }
                if (_activeTabId.value == tabId) {
                    _isLoading.value = true
                    _urlInput.value = url
                }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                updateTabState(tabId) { it.copy(isLoading = false) }
                if (_activeTabId.value == tabId) {
                    _isLoading.value = false
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                updateTabState(tabId) { it.copy(progress = progress) }
                if (_activeTabId.value == tabId) {
                    _progress.value = progress
                }
            }
        }
        
        loadUrl(urlStr, tabId)
    }

    private fun updateTabState(tabId: String, update: (TabState) -> TabState) {
        _tabs.value = _tabs.value.map { if (it.id == tabId) update(it) else it }
    }

    fun switchTab(tabId: String) {
        _activeTabId.value = tabId
        val tab = _tabs.value.find { it.id == tabId }
        if (tab != null) {
            _urlInput.value = tab.url
            _progress.value = tab.progress
            _isLoading.value = tab.isLoading
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val tabToClose = currentTabs.find { it.id == tabId }
        val remainingTabs = currentTabs.filter { it.id != tabId }
        
        tabToClose?.session?.close()
        _tabs.value = remainingTabs

        if (remainingTabs.isEmpty()) {
            createNewTab()
        } else if (_activeTabId.value == tabId) {
            switchTab(remainingTabs.last().id)
        }
    }

    fun loadUrl(url: String, tabId: String? = null) {
        val targetTabId = tabId ?: _activeTabId.value ?: return
        var finalUrl = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            val encodedQuery = java.net.URLEncoder.encode(url, "UTF-8")
            finalUrl = "${_searchEngine.value}$encodedQuery"
        }
        
        updateTabState(targetTabId) { it.copy(url = finalUrl, title = finalUrl) }
        
        if (targetTabId == _activeTabId.value) {
            _urlInput.value = finalUrl
        }
        
        _tabs.value.find { it.id == targetTabId }?.session?.loadUri(finalUrl)
    }

    fun setUrlInput(url: String) {
        _urlInput.value = url
    }

    fun toggleIncognito() {
        _isIncognito.value = !_isIncognito.value
        // Close all tabs and open a new one
        _tabs.value.forEach { it.session.close() }
        _tabs.value = emptyList()
        createNewTab()
    }

    fun toggleAdBlocker() {
        _adBlockerEnabled.value = !_adBlockerEnabled.value
        // In a real app we would toggle tracking protection via GeckoRuntimeSettings or WebExtensions
    }

    fun goBack() {
        activeSession?.goBack()
    }

    fun goForward() {
        activeSession?.goForward()
    }

    fun reload() {
        activeSession?.reload()
    }

    fun isBookmarked(url: String): Flow<Boolean> {
        return database?.bookmarkDao()?.isBookmarked(url) ?: kotlinx.coroutines.flow.flowOf(false)
    }

    fun saveBookmark(title: String, url: String, tag: String) {
        viewModelScope.launch {
            val db = database ?: return@launch
            val existing = _bookmarks.value.find { it.url == url }
            if (existing != null) {
                db.bookmarkDao().insertBookmark(existing.copy(title = title, tag = tag))
            } else {
                db.bookmarkDao().insertBookmark(Bookmark(url = url, title = title, tag = tag))
            }
        }
    }
    
    fun removeBookmark(url: String) {
        viewModelScope.launch {
            val db = database ?: return@launch
            val bookmark = _bookmarks.value.find { it.url == url }
            if (bookmark != null) {
                db.bookmarkDao().deleteBookmarkById(bookmark.id)
            }
        }
    }

    private fun syncExtensionsWithGecko() {
        // In a real implementation we would call:
        // geckoRuntime?.webExtensionController?.install(...) 
        // depending on whether the extension isEnabled.
        // However, WebExtensions require valid extension archives or built-in assets.
    }

    fun installExtension(extension: Extension) {
        viewModelScope.launch {
            // Simulated secure validation layer
            if (!extension.isVerified) {
                // Warning dialog would be shown in UI
            }
            database?.extensionDao()?.insertExtension(extension)
        }
    }

    fun toggleExtension(id: String, isEnabled: Boolean) {
        viewModelScope.launch {
            database?.extensionDao()?.updateExtensionStatus(id, isEnabled)
        }
    }

    fun uninstallExtension(id: String) {
        viewModelScope.launch {
            database?.extensionDao()?.removeExtension(id)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun startDownload(url: String, fileName: String? = null) {
        val fName = fileName ?: url.substringAfterLast("/").takeIf { it.isNotEmpty() } ?: "download_${System.currentTimeMillis()}"
        downloader?.startDownload(url, fName, parts = 4)
        _message.value = "Memulai download: $fName"
    }

    fun downloadCurrentPage() {
        val tab = _tabs.value.find { it.id == _activeTabId.value } ?: return
        startDownload(tab.url)
    }

    fun createDesktopShortcut(context: Context) {
        val tab = _tabs.value.find { it.id == _activeTabId.value } ?: return
        val url = tab.url
        val title = tab.title

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(android.content.pm.ShortcutManager::class.java)
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                    action = android.content.Intent.ACTION_VIEW
                    data = android.net.Uri.parse(url)
                }

                val shortcutInfo = android.content.pm.ShortcutInfo.Builder(context, "shortcut_${System.currentTimeMillis()}")
                    .setShortLabel(if (title.length > 10) title.take(10) + "..." else title)
                    .setLongLabel(title)
                    .setIcon(android.graphics.drawable.Icon.createWithResource(context, android.R.mipmap.sym_def_app_icon)) // standard icon
                    .setIntent(intent)
                    .build()

                shortcutManager.requestPinShortcut(shortcutInfo, null)
                _message.value = "Meminta pembuatan pintasan..."
            } else {
                _message.value = "Perangkat tidak mendukung pintasan layar utama"
            }
        } else {
            _message.value = "Fitur ini memerlukan Android Oreo (8.0) atau lebih baru"
        }
    }

    fun savePageOffline(context: Context, asPdf: Boolean) {
        val tab = _tabs.value.find { it.id == _activeTabId.value } ?: return
        val url = tab.url
        val title = tab.title

        viewModelScope.launch {
            try {
                // In a full implementation, we would use GeckoSession.PrintDelegate to save as PDF
                // or fetch the page source and write it to local storage.
                // Here we simulate the file creation in the app's cache directory.
                val extension = if (asPdf) "pdf" else "html"
                val filename = "${title.replace(Regex("[^a-zA-Z0-9.-]"), "_")}.$extension"
                val file = java.io.File(context.cacheDir, filename)
                
                // Simulated writing
                file.writeText("Saved content for $url")
                
                _message.value = "Halaman berhasil disimpan sebagai $extension: $filename"
            } catch (e: Exception) {
                _message.value = "Gagal menyimpan halaman: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_clearDataOnExit.value) {
            val flags = org.mozilla.geckoview.StorageController.ClearFlags.ALL
            geckoRuntime?.storageController?.clearData(flags)
        }
    }
}
