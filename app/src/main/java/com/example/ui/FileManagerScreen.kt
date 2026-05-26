package com.example.ui

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManagerViewModel : ViewModel() {
    var leftPath by mutableStateOf<File>(Environment.getExternalStorageDirectory())
    var rightPath by mutableStateOf<File>(Environment.getExternalStorageDirectory())
    
    var leftFiles by mutableStateOf<List<File>>(emptyList())
    var rightFiles by mutableStateOf<List<File>>(emptyList())

    var activePane by mutableStateOf("Left") // "Left" or "Right"
    var selectedFile by mutableStateOf<File?>(null)
    
    init {
        loadFiles("Left")
        loadFiles("Right")
    }
    
    fun loadFiles(pane: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = if (pane == "Left") leftPath else rightPath
            val list = path.listFiles()?.toList() ?: emptyList()
            val sortedList = list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            withContext(Dispatchers.Main) {
                if (pane == "Left") leftFiles = sortedList else rightFiles = sortedList
            }
        }
    }

    fun navigateTo(file: File, pane: String) {
        if (file.isDirectory) {
            if (pane == "Left") {
                leftPath = file
            } else {
                rightPath = file
            }
            loadFiles(pane)
        } else {
            selectedFile = file
            activePane = pane
        }
    }

    fun navigateUp(pane: String) {
        val path = if (pane == "Left") leftPath else rightPath
        path.parentFile?.let { parent ->
            if (parent.exists() && parent.canRead()) {
                if (pane == "Left") leftPath = parent else rightPath = parent
                loadFiles(pane)
            }
        }
    }
    
    fun copySelected() {
        val source = selectedFile ?: return
        val targetPath = if (activePane == "Left") rightPath else leftPath
        val dest = File(targetPath, source.name)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (source.isDirectory) {
                    source.copyRecursively(dest, overwrite = true)
                } else {
                    source.copyTo(dest, overwrite = true)
                }
                loadFiles(if (activePane == "Left") "Right" else "Left")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun deleteSelected() {
        val file = selectedFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                loadFiles(activePane)
                selectedFile = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(onBack: () -> Unit) {
    val viewModel: FileManagerViewModel = viewModel()
    
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MT Manager Clone") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Close") }
                },
                actions = {
                    if (viewModel.selectedFile != null) {
                        IconButton(onClick = { viewModel.copySelected() }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                        IconButton(onClick = { viewModel.deleteSelected() }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                    }
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Refresh") },
                            onClick = { 
                                viewModel.loadFiles("Left")
                                viewModel.loadFiles("Right")
                                showMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Dual Pane Layout
            Row(modifier = Modifier.weight(1f)) {
                // LEFT PANE
                FilePane(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    path = viewModel.leftPath,
                    files = viewModel.leftFiles,
                    isActive = viewModel.activePane == "Left",
                    selectedFile = viewModel.selectedFile,
                    onNavigateUp = { viewModel.navigateUp("Left") },
                    onNavigateTo = { viewModel.navigateTo(it, "Left") },
                    onSelectPane = { viewModel.activePane = "Left" }
                )
                
                Divider(modifier = Modifier.width(2.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.primary)
                
                // RIGHT PANE
                FilePane(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    path = viewModel.rightPath,
                    files = viewModel.rightFiles,
                    isActive = viewModel.activePane == "Right",
                    selectedFile = viewModel.selectedFile,
                    onNavigateUp = { viewModel.navigateUp("Right") },
                    onNavigateTo = { viewModel.navigateTo(it, "Right") },
                    onSelectPane = { viewModel.activePane = "Right" }
                )
            }
        }
    }
}

@Composable
fun FilePane(
    modifier: Modifier = Modifier,
    path: File,
    files: List<File>,
    isActive: Boolean,
    selectedFile: File?,
    onNavigateUp: () -> Unit,
    onNavigateTo: (File) -> Unit,
    onSelectPane: () -> Unit
) {
    Column(
        modifier = modifier
            .background(if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .clickable { onSelectPane() }
    ) {
        // Path header
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateUp, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = path.absolutePath, 
                style = MaterialTheme.typography.labelSmall, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
        }
        Divider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (path.parentFile != null) {
                 item {
                     Row(
                         modifier = Modifier.fillMaxWidth().clickable { onNavigateUp() }.padding(8.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(modifier = Modifier.width(8.dp))
                         Text("..")
                     }
                     Divider()
                 }
            }
            items(files) { file ->
                val isSelected = (file == selectedFile)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { 
                            onSelectPane()
                            onNavigateTo(file) 
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!file.isDirectory) {
                            Text(
                                "${file.length() / 1024} KB • ${SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(file.lastModified()))}", 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Divider()
            }
        }
    }
}
