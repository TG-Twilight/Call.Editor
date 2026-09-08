package com.twilight.calleditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal const val PROJECT_URL = "https://github.com/TG-Twilight/Call.Editor"

private data class Contributor(val name: String, val description: String, val avatar: Int, val url: String)
private val contributors = listOf(
    Contributor("GPT-6-Astra", "AI 开发协作", R.drawable.avatar_openai, "https://openai.com/index/gpt-6-astra/")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onOpenLink: (String) -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        TopAppBar(windowInsets = WindowInsets(0, 0, 0, 0), title = { Text("关于") },
            navigationIcon = { IconButton(onClick = onBack) { AppSymbol(AppIcon.Back, "返回") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Surface(shape = RoundedCornerShape(36.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExpressiveBadge(AppIcon.Calls, prominent = true)
                        Text("Call.Editor", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("原生通话记录编辑与备份工具", style = MaterialTheme.typography.bodyLarge)
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                            Text("${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item { Text("项目", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            item {
                Card(onClick = { onOpenLink(PROJECT_URL) }, shape = connectedShape(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("GitHub 项目", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                            AppSymbol(AppIcon.Outgoing)
                        }
                        Text("TG-Twilight / Call.Editor", style = MaterialTheme.typography.bodyLarge)
                        Text("查看源码、提交问题或参与贡献", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { Text("贡献者", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            items(contributors, key = { it.name }) { contributor ->
                Card(onClick = { onOpenLink(contributor.url) }, shape = connectedShape(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Image(painterResource(contributor.avatar), contentDescription = "${contributor.name} 头像", modifier = Modifier.size(56.dp).clip(CircleShape))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(contributor.name, style = MaterialTheme.typography.titleMedium)
                            Text(contributor.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AppSymbol(AppIcon.Outgoing)
                    }
                }
            }
            item { Text("本机处理 · 无广告\n外部链接由浏览器打开", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
