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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditorScreen(original: CallEntry, busy: Boolean, onBack: () -> Unit, onSave: (CallEntry, Boolean?) -> Unit, onDelete: (() -> Unit)?, vendorSupported: Boolean = false) {
    val context = LocalContext.current
    var number by rememberSaveable(original.id) { mutableStateOf(original.number) }
    var name by rememberSaveable(original.id) { mutableStateOf(original.name) }
    var duration by rememberSaveable(original.id) { mutableStateOf(original.duration.toString()) }
    var type by rememberSaveable(original.id) { mutableIntStateOf(original.type) }
    var date by rememberSaveable(original.id) { mutableLongStateOf(original.date) }
    val originalBreeno = original.isBreenoCall(vendorSupported)
    var breeno by rememberSaveable(original.id) { mutableStateOf(originalBreeno) }
    val canEditBreeno = vendorSupported && original.id != 0L && original.vendorDetails?.features != null && !original.vendorDetails.virtualCallId.isNullOrEmpty()
    var error by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val cal = remember(date) { Calendar.getInstance().apply { timeInMillis = date } }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = java.time.Instant.ofEpochMilli(date).atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(), yearRange = 1900..10000)
    val timeState = rememberTimePickerState(initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE), is24Hour = true)
    val dirty = number != original.number || name != original.name || duration != original.duration.toString() || type != original.type || date != original.date || breeno != originalBreeno
    fun leave() { if (!busy) { if (dirty) discard = true else onBack() } }
    BackHandler { leave() }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(windowInsets = WindowInsets(0, 0, 0, 0), title = { Text(if (original.id == 0L) "新增记录" else "编辑记录") }, navigationIcon = { TextButton(enabled = !busy, onClick = { leave() }) { Text("取消") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("通话详情", style = MaterialTheme.typography.headlineMedium)
            Text("保存后会更新系统中的通话记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(number, { number = it }, label = { Text("电话号码") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(name, { name = it }, label = { Text("显示姓名（可选）") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Text("通话类型", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { editableCallTypes(original.type).forEach { value -> FilterChip(selected = type == value, onClick = { type = value }, enabled = !busy, label = { Text(typeLabel(value)) }) } }
            Text("修改这里只改变通话记录，不会更改系统黑名单；拦截分类不代表电话当时一定未接通。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (vendorSupported) {
                Text("小布代接", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (breeno) "已标记为小布代接" else "未标记为小布代接", modifier = Modifier.weight(1f))
                    Switch(checked = breeno, onCheckedChange = { breeno = it }, enabled = !busy && canEditBreeno,
                        modifier = Modifier.semantics { contentDescription = "小布代接标记" })
                }
                Text(if (canEditBreeno) "仅修改已有记录的代接标记，保留关联编号和其他标志；不会生成或验证小布文字、录音。当前基本字段备份不包含此标记及关联内容。"
                    else "仅有小布标志和关联编号的记录才会识别为代接。此记录没有可编辑的关联信息，无法凭空创建小布内容。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("日期与时间", style = MaterialTheme.typography.titleMedium)
            FilledTonalButton(enabled = !busy, onClick = {
                pickDate = true
            }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) { Text(dateLabel(date, "yyyy-MM-dd HH:mm:ss")) }
            OutlinedTextField(duration, { duration = it }, label = { Text("通话时长（秒）") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp))
            original.accountId?.let { Text("SIM / 电话账户：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight), enabled = !busy, onClick = {
                val seconds = duration.toLongOrNull()
                if (number.isBlank() && (original.id == 0L || original.number.isNotBlank())) error = "请输入电话号码"
                else if (seconds == null) error = "请输入有效的秒数"
                else {
                    val value = original.copy(number = if (number == original.number) number else number.trim(), name = if (name == original.name) name else name.trim(), duration = seconds, type = type, date = date)
                    error = value.validate()
                    if (error == null) onSave(value, if (breeno != originalBreeno) breeno else null)
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (busy) "正在保存…" else "保存记录", style = MaterialTheme.typography.titleMedium) }
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
