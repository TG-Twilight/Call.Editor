package com.twilight.calleditor

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionPanel(modifier: Modifier = Modifier, busy: Boolean, onPermission: () -> Unit, onSettings: () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("允许访问通话记录", style = MaterialTheme.typography.headlineSmall) }
        item { Text("查看、编辑和备份需要通话记录权限。授权后即可在这里管理系统中的记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Button(enabled = !busy, onClick = onPermission, modifier = Modifier.fillMaxWidth()) { Text("授予通话记录权限") } }
        item { TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("打开系统权限设置") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(count: Int, permission: Boolean, busy: Boolean, onPermission: () -> Unit, onSettings: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = { MediumTopAppBar(windowInsets = WindowInsets(0, 0, 0, 0), title = { Text("备份与恢复", fontWeight = FontWeight.Bold) }) }) { padding ->
        if (!permission) { PermissionPanel(Modifier.padding(padding), busy, onPermission, onSettings); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("通话记录", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            item {
                Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BackupCardHeading(AppIcon.Backup, "导出备份")
                        Text("将当前 $count 条通话记录保存为 JSON 文件，自行选择存放位置。", style = MaterialTheme.typography.bodyMedium)
                        ExpressiveAction("选择位置并导出", enabled = !busy, onClick = onExport, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BackupCardHeading(AppIcon.Restore, "从文件恢复")
                        Text("先检查文件并预览数量，再追加到系统记录。完全相同的记录自动跳过。", style = MaterialTheme.typography.bodyMedium)
                        ExpressiveAction("选择备份文件", enabled = !busy, onClick = onImport, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item { Text("备份包含哪些内容", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            item { Text("号码、显示姓名、通话时间、时长、类型与电话账户。文件不包含录音、联系人通讯录或短信。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Text("文件以明文保存，请存放在可信位置。目前支持本应用导出的通话备份，旧版 Flutter 私有备份尚不兼容。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(preferences: AppPreferences, saving: Boolean, permission: Boolean, onChange: (AppPreferences) -> Unit, onPermission: () -> Unit, onSettings: () -> Unit, onAbout: () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = { MediumTopAppBar(windowInsets = WindowInsets(0, 0, 0, 0), title = { Text("设置", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { SectionLabel("外观") }
            item {
                SettingsGroup(shape = connectedShape(last = false)) {
                    Text("主题模式", style = MaterialTheme.typography.titleMedium)
                    ThemeChoices(preferences.theme, !saving) { onChange(preferences.copy(theme = it)) }
                }
                Spacer(Modifier.height(4.dp))
                SettingsGroup(shape = connectedShape(first = false)) {
                    ToggleSetting("动态配色", if (Build.VERSION.SDK_INT >= 31) "使用系统壁纸的颜色" else "需要 Android 12 或更高版本", preferences.dynamicColor, !saving && Build.VERSION.SDK_INT >= 31) { onChange(preferences.copy(dynamicColor = it)) }
                }
            }
            item { SectionLabel("记录列表") }
            item { SettingsGroup { ToggleSetting("紧凑列表", "缩小行间距，在一屏显示更多记录", preferences.compactList, !saving) { onChange(preferences.copy(compactList = it)) } } }
            item { SectionLabel("权限与数据") }
            item {
                SettingsGroup {
                    Text("通话记录权限", style = MaterialTheme.typography.titleMedium)
                    Text(if (permission) "已允许读取与修改" else "未获得完整权限", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!permission) FilledTonalButton(onClick = onPermission) { Text("授予通话记录权限") }
                    TextButton(onClick = onSettings) { Text("打开系统应用设置") }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("记录保存在系统通话记录中。编辑和删除会同步到系统；导出文件由你自行保存和管理。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Surface(onClick = onAbout, shape = connectedShape(),
                    color = settingsContainerColor(),
                    contentColor = MaterialTheme.colorScheme.onSurface) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExpressiveBadge(AppIcon.Info)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("关于", style = MaterialTheme.typography.titleLarge)
                            Text("${BuildConfig.VERSION_NAME} · 项目与贡献者", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AppSymbol(AppIcon.Forward)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) { Text(text, modifier = Modifier.padding(start = 12.dp, top = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }

@Composable
private fun BackupCardHeading(icon: AppIcon, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppSymbol(icon, modifier = Modifier.size(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun SettingsGroup(shape: androidx.compose.ui.graphics.Shape = connectedShape(), content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = shape, color = settingsContainerColor(), contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun ToggleSetting(title: String, description: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
