package com.twilight.calleditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(original: CallEntry, busy: Boolean, onBack: () -> Unit, onSave: (CallEntry) -> Unit, onDelete: (() -> Unit)?) {
    val context = LocalContext.current
    var number by rememberSaveable(original.id) { mutableStateOf(original.number) }
    var name by rememberSaveable(original.id) { mutableStateOf(original.name) }
    var duration by rememberSaveable(original.id) { mutableStateOf(original.duration.toString()) }
    var type by rememberSaveable(original.id) { mutableIntStateOf(original.type) }
    var date by rememberSaveable(original.id) { mutableLongStateOf(original.date) }
    var error by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val cal = remember(date) { Calendar.getInstance().apply { timeInMillis = date } }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = java.time.Instant.ofEpochMilli(date).atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(), yearRange = 1900..10000)
    val timeState = rememberTimePickerState(initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE), is24Hour = true)
    val dirty = number != original.number || name != original.name || duration != original.duration.toString() || type != original.type || date != original.date
    fun leave() { if (!busy) { if (dirty) discard = true else onBack() } }
    BackHandler { leave() }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(windowInsets = WindowInsets(0, 0, 0, 0), title = { Text(if (original.id == 0L) "新增记录" else "编辑记录") }, navigationIcon = { TextButton(enabled = !busy, onClick = { leave() }) { Text("取消") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("通话详情", style = MaterialTheme.typography.headlineMedium)
            Text("保存后会更新系统中的通话记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(number, { number = it }, label = { Text("电话号码") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(name, { name = it }, label = { Text("显示姓名（可选）") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Text("通话类型", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ((1..7).toList() + if (original.type !in 1..7) listOf(original.type) else emptyList()).forEach { value -> FilterChip(selected = type == value, onClick = { type = value }, enabled = !busy, label = { Text(typeLabel(value)) }) } }
            Text("日期与时间", style = MaterialTheme.typography.titleMedium)
            FilledTonalButton(enabled = !busy, onClick = {
                pickDate = true
            }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) { Text(dateLabel(date, "yyyy-MM-dd HH:mm:ss")) }
            OutlinedTextField(duration, { duration = it }, label = { Text("通话时长（秒）") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp))
            original.accountId?.let { Text("SIM / 电话账户：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = !busy, onClick = {
                val seconds = duration.toLongOrNull()
                if (number.isBlank() && (original.id == 0L || original.number.isNotBlank())) error = "请输入电话号码"
                else if (seconds == null) error = "请输入有效的秒数"
                else {
                    val value = original.copy(number = if (number == original.number) number else number.trim(), name = if (name == original.name) name else name.trim(), duration = seconds, type = type, date = date)
                    error = value.validate()
                    if (error == null) onSave(value)
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (busy) "正在保存…" else "保存记录") }
            onDelete?.let { TextButton(enabled = !busy, onClick = it, modifier = Modifier.fillMaxWidth()) { Text("删除这条记录", color = MaterialTheme.colorScheme.error) } }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("放弃未保存的修改？") }, confirmButton = { TextButton(onClick = onBack) { Text("放弃修改") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("继续编辑") } })
    if (pickDate) DatePickerDialog(onDismissRequest = { pickDate = false }, confirmButton = {
        TextButton(enabled = dateState.selectedDateMillis != null, onClick = { pickDate = false; pickTime = true }) { Text("选择时间") }
    }, dismissButton = { TextButton(onClick = { pickDate = false }) { Text("取消") } }) { DatePicker(state = dateState) }
    if (pickTime) AlertDialog(onDismissRequest = { pickTime = false }, title = { Text("通话时间") }, text = { TimeInput(state = timeState) }, confirmButton = {
        TextButton(onClick = {
            val day = java.time.Instant.ofEpochMilli(dateState.selectedDateMillis!!).atZone(java.time.ZoneOffset.UTC).toLocalDate()
            date = day.atTime(timeState.hour, timeState.minute, cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND) * 1000000).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            pickTime = false
        }) { Text("确定") }
    }, dismissButton = { TextButton(onClick = { pickTime = false }) { Text("取消") } })
}
