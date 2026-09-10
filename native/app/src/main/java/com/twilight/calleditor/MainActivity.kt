package com.twilight.calleditor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)
    override fun onResume() { super.onResume(); resumeTick++ }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { PreferenceStore(applicationContext) }
            var preferences by remember { mutableStateOf<AppPreferences?>(null) }
            var saving by remember { mutableStateOf(false) }
            var preferenceError by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) { preferences = withContext(Dispatchers.IO) { store.load() } }
            preferences?.let { current ->
                CallTheme(current) {
                    App(resumeTick, current, saving) { next ->
                        if (!saving) scope.launch {
                            saving = true
                            try { withContext(Dispatchers.IO) { store.save(next) }; preferences = next }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { preferenceError = "设置保存失败，请重试" }
                            finally { saving = false }
                        }
                    }
                    preferenceError?.let { AlertDialog(onDismissRequest = { preferenceError = null }, title = { Text("设置未保存") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { preferenceError = null }) { Text("知道了") } }) }
                }
            }
        }
    }

    @Composable
    private fun App(resume: Int, preferences: AppPreferences, savingPreferences: Boolean, onPreferences: (AppPreferences) -> Unit) {
        val repo = remember { CallRepository(applicationContext) }
        val scope = rememberCoroutineScope()
        val operationMutex = remember { Mutex() }
        var entries by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
        var screen by rememberSaveable { mutableStateOf("calls") }
        val pageState = rememberSaveableStateHolder()
        var loading by remember { mutableStateOf(false) }
        var permission by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        val entrySaver = remember { listSaver<CallEntry?, Any>(save = { entry ->
            if (entry == null) emptyList() else listOf(entry.id, entry.number, entry.name, entry.date, entry.duration, entry.type, entry.accountId ?: "", entry.accountId != null,
                entry.vendorDetails != null, entry.vendorDetails?.features ?: 0, entry.vendorDetails?.features != null,
                entry.vendorDetails?.virtualCallId ?: "", entry.vendorDetails?.virtualCallId != null)
        }, restore = { value -> if (value.isEmpty()) null else CallEntry(value[0] as Long, value[1] as String, value[2] as String, value[3] as Long, value[4] as Long, value[5] as Int, if (value[7] as Boolean) value[6] as String else null,
            if (value.size >= 13 && value[8] as Boolean) VendorCallDetails(if (value[10] as Boolean) value[9] as Int else null, if (value[12] as Boolean) value[11] as String else null) else null) }) }
        var edit by rememberSaveable(stateSaver = entrySaver) { mutableStateOf<CallEntry?>(null) }
        var deleting by remember { mutableStateOf<CallEntry?>(null) }
        var imported by remember { mutableStateOf<List<CallEntry>?>(null) }
        var pendingExport by remember { mutableStateOf<String?>(null) }
        val snackbar = remember { SnackbarHostState() }
        fun allowed() = checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        fun runWork(success: String? = null, block: suspend () -> Unit) {
            if (loading) return
            loading = true
            scope.launch {
                operationMutex.withLock {
                    loading = true
                    try { block(); success?.let { scope.launch { snackbar.showSnackbar(it) } } }
                    catch (e: CancellationException) { throw e }
                    catch (e: RestoreException) {
                        message = e.message
                        runCatching { withContext(Dispatchers.IO) { repo.load() } }.getOrNull()?.let { entries = it }
                    }
                    catch (e: Exception) { message = e.message ?: "操作失败，请重试" }
                    finally { loading = false }
                }
            }
        }
        fun refresh() {
            permission = allowed()
            if (permission) runWork { entries = withContext(Dispatchers.IO) { repo.load() } }
            else entries = emptyList()
        }
        val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
        val askPermission = { request.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG)) }
        val systemSettings = { startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        val openLink: (String) -> Unit = { url ->
            try { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (e: android.content.ActivityNotFoundException) { message = "没有可打开此链接的浏览器，请安装浏览器后重试。" }
        }
        val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            uri?.let { runWork("备份已导出") {
                withContext(Dispatchers.IO) {
                    val json = pendingExport ?: repo.exportJson()
                    require(json.toByteArray(Charsets.UTF_8).size <= 20 * 1024 * 1024) { "备份超过 20 MB，无法导出" }
                    (contentResolver.openOutputStream(it, "wt") ?: error("无法打开目标文件")).bufferedWriter(Charsets.UTF_8).use { out -> out.write(json) }
                }
            } }
        }
        val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { runWork {
                imported = withContext(Dispatchers.IO) {
                    val bytes = (contentResolver.openInputStream(it) ?: error("无法读取备份文件")).use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 20 * 1024 * 1024) { "文件超过 20 MB，请选择较小的备份" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    repo.parseBackup(bytes.toString(Charsets.UTF_8))
                }
            } }
        }
        LaunchedEffect(resume) { refresh() }
        BackHandler(enabled = edit == null && screen != "calls") { screen = "calls" }
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            if (edit == null && screen != "about") NavigationBar {
                listOf(Triple("calls", "通话", AppIcon.Calls), Triple("backup", "备份", AppIcon.Backup), Triple("settings", "设置", AppIcon.Settings)).forEach { (key, title, icon) ->
                    NavigationBarItem(selected = screen == key, onClick = { screen = key }, icon = { AppSymbol(icon) }, label = { Text(title) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                when {
                    edit != null -> EditorScreen(edit!!, loading, onBack = { edit = null }, onSave = { entry, breenoEnabled ->
                        val baseline = edit
                        runWork("记录已保存") {
                            withContext(Dispatchers.IO) { repo.save(entry, breenoEnabled, baseline) }
                            edit = null
                            try { entries = withContext(Dispatchers.IO) { repo.load() } }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { message = "记录已保存，但列表刷新失败。请返回列表刷新，不要重复新增。" }
                        }
                    }, onDelete = if (edit!!.id != 0L) ({ deleting = edit }) else null, vendorSupported = repo.vendorSupported)
                    else -> pageState.SaveableStateProvider(screen) {
                        when (screen) {
                            "settings" -> SettingsScreen(preferences, savingPreferences, permission, onPreferences, askPermission, systemSettings, onAbout = { screen = "about" })
                            "about" -> AboutScreen(onBack = { screen = "settings" }, onOpenLink = openLink)
                            "backup" -> BackupScreen(entries.size, permission, loading, askPermission, systemSettings,
                                onExport = { runWork { pendingExport = withContext(Dispatchers.IO) { repo.exportJson() }; export.launch("通话记录-${java.time.LocalDate.now()}.json") } },
                                onImport = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                            else -> CallsScreen(entries, permission, loading, preferences.compactList, askPermission, systemSettings, onRefresh = { refresh() },
                                onEdit = { edit = it }, onAdd = { edit = CallEntry(number = "", date = System.currentTimeMillis(), duration = 0, type = 2) }, vendorSupported = repo.vendorSupported)
                        }
                    }
                }
            }
        }
        message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("操作未完成") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("知道了") } }) }
        deleting?.let { item -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除这条记录？") }, text = { Text("将从系统通话记录中删除。此操作无法撤销。") },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }, confirmButton = { TextButton(enabled = !loading, onClick = {
                deleting = null
                runWork("记录已删除") {
                    withContext(Dispatchers.IO) { repo.delete(item.id) }
                    edit = null
                    try { entries = withContext(Dispatchers.IO) { repo.load() } }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { message = "记录已删除，但列表刷新失败。请返回列表刷新。" }
                }
            }) { Text("删除") } }) }
        imported?.let { items -> AlertDialog(onDismissRequest = { imported = null }, title = { Text("恢复 ${items.size} 条记录？") },
            text = { Text((if (items.isEmpty()) "这是一个空备份。\n\n" else "日期范围：${dateLabel(items.minOf { it.date }, "yyyy-MM-dd")} 至 ${dateLabel(items.maxOf { it.date }, "yyyy-MM-dd")}\n\n") + "追加到系统通话记录，完全相同的记录会跳过。现有记录不会被清空。") },
            dismissButton = { TextButton(onClick = { imported = null }) { Text("取消") } }, confirmButton = { TextButton(enabled = !loading, onClick = {
                imported = null
                runWork {
                    val result = withContext(Dispatchers.IO) { repo.restore(items) }
                    entries = withContext(Dispatchers.IO) { repo.load() }
                    scope.launch { snackbar.showSnackbar("已恢复 ${result.inserted} 条，跳过 ${result.skipped} 条") }
                }
            }) { Text("恢复") } }) }
    }
}
