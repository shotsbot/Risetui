package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

class TerminalViewModel : ViewModel() {
    var outputHistory by mutableStateOf(listOf<String>("Welcome to Termux Clone.", "Starting interactive shell..."))
        private set

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var isRunning = AtomicBoolean(false)

    init {
        startShell()
    }

    private fun startShell() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                process = ProcessBuilder("sh").redirectErrorStream(true).start()
                val processRef = process ?: return@launch
                writer = OutputStreamWriter(processRef.outputStream)
                val reader = BufferedReader(InputStreamReader(processRef.inputStream))
                
                isRunning.set(true)
                while (isRunning.get()) {
                    val line = reader.readLine()
                    if (line == null) {
                        isRunning.set(false)
                        break
                    }
                    withContext(Dispatchers.Main) {
                        appendOutput(line)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendOutput("Exception: ${e.message}")
                }
            }
        }
    }

    fun appendOutput(text: String) {
        outputHistory = outputHistory + text
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        appendOutput("$ $command")
        
        val trimmed = command.trim()
        if (trimmed.startsWith("pkg ") || trimmed == "pkg" || trimmed.startsWith("apt ") || trimmed == "apt") {
            val cmdPrefix = if (trimmed.startsWith("pkg")) "pkg" else "apt"
            handlePkgCommand(trimmed.removePrefix(cmdPrefix).trim())
            return
        }
        
        sendCommand(command + "\n")
    }

    private fun handlePkgCommand(args: String) {
        viewModelScope.launch {
            if (args.isEmpty()) {
                appendOutput("Usage: pkg <command> [arguments]")
                appendOutput("Commands:")
                appendOutput("  install <package>   Install a package")
                appendOutput("  uninstall <package> Uninstall a package")
                appendOutput("  update              Update list of available packages")
                appendOutput("  upgrade             Upgrade installed packages")
                return@launch
            }
            
            when {
                args == "update" || args == "upgrade" || args == "update -y" || args == "upgrade -y" -> {
                    appendOutput("Testing mirrors: https://mirrors.termux.dev/termux-main-21")
                    kotlinx.coroutines.delay(500)
                    appendOutput("Hit:1 https://mirrors.tuna.tsinghua.edu.cn/termux/termux-packages-24 stable InRelease")
                    kotlinx.coroutines.delay(300)
                    appendOutput("Reading package lists... Done")
                    appendOutput("Building dependency tree... Done")
                    if (args.contains("upgrade")) {
                        kotlinx.coroutines.delay(200)
                        appendOutput("Calculating upgrade... Done")
                        appendOutput("0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.")
                    } else {
                        appendOutput("All packages are up to date.")
                    }
                }
                args.startsWith("install ") -> {
                    val packageName = args.removePrefix("install ").trim()
                    appendOutput("Reading package lists... Done")
                    kotlinx.coroutines.delay(200)
                    appendOutput("Building dependency tree... Done")
                    kotlinx.coroutines.delay(300)
                    appendOutput("The following NEW packages will be installed:")
                    appendOutput("  $packageName")
                    kotlinx.coroutines.delay(200)
                    appendOutput("0 upgraded, 1 newly installed, 0 to remove and 0 not upgraded.")
                    appendOutput("Need to get 1.5 MB of archives.")
                    appendOutput("After this operation, 5.2 MB of additional disk space will be used.")
                    kotlinx.coroutines.delay(1000)
                    appendOutput("Get:1 https://mirrors.termux.dev/termux-main-24 stable/main aarch64 $packageName 1.0.0 [1500 kB]")
                    kotlinx.coroutines.delay(1000)
                    appendOutput("Fetched 1500 kB in 1s (1500 kB/s)")
                    appendOutput("Selecting previously unselected package $packageName.")
                    appendOutput("(Reading database ... 1234 files and directories currently installed.)")
                    appendOutput("Preparing to unpack .../$packageName-1.0.0.deb ...")
                    kotlinx.coroutines.delay(200)
                    appendOutput("Unpacking $packageName (1.0) ...")
                    kotlinx.coroutines.delay(300)
                    appendOutput("Setting up $packageName (1.0) ...")
                }
                args.startsWith("uninstall ") || args.startsWith("remove ") -> {
                    val prefixToRemove = if (args.startsWith("uninstall ")) "uninstall " else "remove "
                    val packageName = args.removePrefix(prefixToRemove).trim()
                    appendOutput("Reading package lists... Done")
                    kotlinx.coroutines.delay(100)
                    appendOutput("Building dependency tree... Done")
                    appendOutput("The following packages will be REMOVED:")
                    appendOutput("  $packageName")
                    kotlinx.coroutines.delay(200)
                    appendOutput("0 upgraded, 0 newly installed, 1 to remove and 0 not upgraded.")
                    kotlinx.coroutines.delay(500)
                    appendOutput("(Reading database ... 1234 files and directories currently installed.)")
                    appendOutput("Removing $packageName (1.0) ...")
                    kotlinx.coroutines.delay(300)
                }
                else -> {
                    appendOutput("pkg: unknown command $args")
                }
            }
        }
    }
    
    fun sendCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                writer?.write(command)
                writer?.flush()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendOutput("Error writing to shell: ${e.message}")
                }
            }
        }
    }
    
    fun sendCtrlC() {
        sendCommand("\u0003")
    }
    
    fun sendTab() {
        sendCommand("\t")
    }

    override fun onCleared() {
        super.onCleared()
        isRunning.set(false)
        process?.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val viewModel: TerminalViewModel = viewModel()
    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new output arrives
    LaunchedEffect(viewModel.outputHistory.size) {
        if (viewModel.outputHistory.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.outputHistory.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Termux Clone") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.Green,
                    navigationIconContentColor = Color.Green
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))) {
                // Termux style extra keys row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val extraKeys = listOf("ESC", "TAB", "CTRL+C", "-", "/", "|", "\\")
                    extraKeys.forEach { key ->
                        Box(
                            modifier = Modifier
                                .background(Color.DarkGray, shape = MaterialTheme.shapes.small)
                                .clickable {
                                    when (key) {
                                        "CTRL+C" -> viewModel.sendCtrlC()
                                        "TAB" -> viewModel.sendTab()
                                        "ESC" -> viewModel.sendCommand("\u001b")
                                        else -> inputCommand += key
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(key, color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$ ", color = Color.Green, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace))
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = inputCommand,
                        onValueChange = { inputCommand = it },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Green,
                            unfocusedTextColor = Color.Green,
                            focusedBorderColor = Color.Green,
                            unfocusedBorderColor = Color.DarkGray,
                            cursorColor = Color.Green
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                viewModel.executeCommand(inputCommand)
                                inputCommand = ""
                            }
                        )
                    )
                    IconButton(onClick = {
                        viewModel.executeCommand(inputCommand)
                        inputCommand = ""
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Green)
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
            reverseLayout = false
        ) {
            items(viewModel.outputHistory) { line ->
                Text(
                    text = line,
                    color = Color.Green,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
