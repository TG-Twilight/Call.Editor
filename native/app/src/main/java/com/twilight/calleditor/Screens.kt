package com.twilight.calleditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun typeLabel(type: Int) = when (type) { 1 -> "呼入"; 2 -> "呼出"; 3 -> "未接"; 4 -> "语音信箱"; 5 -> "已拒接"; 6 -> "已拦截"; 7 -> "其他设备接听"; else -> "其他（$type）" }
fun dateLabel(date: Long, pattern: String = "MM月dd日 HH:mm") = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(entries: List<CallEntry>, permission: Boolean, busy: Boolean, compact: Boolean, onPermission: () -> Unit, onSettings: () -> Unit, onRefresh: () -> Unit, onEdit: (CallEntry) -> Unit, onAdd: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val shown = remember(entries, query, filter) { entries.filter { (filter == 0 || it.type == filter || (filter == -100 && it.type !in 1..7)) && (it.number.contains(query, true) || it.name.contains(query, true)) } }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = { MediumTopAppBar(windowInsets = WindowInsets(0, 0, 0, 0), title = { Text("通话记录", fontWeight = FontWeight.Bold) }, actions = { IconButton(enabled = !busy && permission, onClick = onRefresh) { AppSymbol(AppIcon.Refresh, "刷新") } }) },
        floatingActionButton = { if (permission) ExtendedFloatingActionButton(modifier = Modifier.semantics { contentDescription = "新增记录" }, onClick = { if (!busy) onAdd() }, icon = { AppSymbol(AppIcon.Add) }, text = { Text("新增记录") }) }) { padding ->
        if (!permission) {
            PermissionPanel(Modifier.padding(padding), busy, onPermission, onSettings)
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), placeholder = { Text("搜索姓名或号码") }, leadingIcon = { AppSymbol(AppIcon.Search) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { AppSymbol(AppIcon.Close, "清除搜索") } }, singleLine = true, shape = RoundedCornerShape(28.dp)) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) { (listOf(0 to "全部") + (1..7).map { it to typeLabel(it) } + listOf(-100 to "其他类型")).forEach { (value, label) -> FilterChip(selected = value == filter, onClick = { filter = value }, label = { Text(label) }) } } }
            item { Text(if (query.isEmpty() && filter == 0) "共 ${entries.size} 条记录" else "找到 ${shown.size} 条 · 共 ${entries.size} 条", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
            if (shown.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(if (busy) "正在读取记录…" else if (query.isNotEmpty() || filter != 0) "没有匹配的记录" else "还没有通话记录", style = MaterialTheme.typography.titleLarge); if (!busy) Text(if (query.isNotEmpty() || filter != 0) "试试其他搜索或筛选条件" else "点击“新增记录”创建第一条记录", style = MaterialTheme.typography.bodyMedium) } }
            shown.groupBy { dateLabel(it.date, "yyyy年MM月dd日") }.forEach { (day, calls) ->
                item(key = "day-$day") { Text(day, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) }
                itemsIndexed(calls, key = { _, call -> call.id }) { index, call ->
                    Surface(onClick = { onEdit(call) }, enabled = !busy, shape = connectedShape(first = index == 0, last = index == calls.lastIndex), color = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(modifier = Modifier.size(if (compact) 40.dp else 48.dp), shape = RoundedCornerShape(if (call.type == 3) 16.dp else 24.dp), color = if (call.type == 3) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer, contentColor = if (call.type == 3) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer) { Box(contentAlignment = Alignment.Center) { AppSymbol(when (call.type) { 2 -> AppIcon.Outgoing; 1, 3 -> AppIcon.Incoming; else -> AppIcon.Other }) } }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(call.name.ifBlank { call.number.ifBlank { "隐藏号码" } }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (call.name.isNotBlank()) Text(call.number.ifBlank { "隐藏号码" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${typeLabel(call.type)} · ${call.duration / 60}分${call.duration % 60}秒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(dateLabel(call.date, "HH:mm"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
