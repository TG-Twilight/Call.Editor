package com.twilight.calleditor

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

data class CallEntry(
    val id: Long = 0,
    val number: String,
    val name: String = "",
    val date: Long,
    val duration: Long,
    val type: Int,
    val accountId: String? = null,
    val vendorDetails: VendorCallDetails? = null,
)

// Raw values observed on RMX3031 / Android 15 and RMX5200 / Android 16.
// Preserve raw values. Meanings for the verified RMX5200 profile are in CallStatus.kt.
internal fun isSupportedCallType(type: Int): Boolean =
    type in 1..7 || type == -5 || type == -3 || type == -1 || type == 27 || type == 51 || type == 52

fun CallEntry.validate(): String? = when {
    id < 0 -> "记录编号无效"
    date !in 1..253402300799999L -> "通话日期无效，请使用 1970 至 9999 年之间的日期"
    duration < 0 -> "通话时长不能为负数"
    !isSupportedCallType(type) -> "通话类型无效（原始值：$type）"
    number.length > 1024 -> "电话号码过长"
    name.length > 10000 -> "姓名过长"
    (accountId?.length ?: 0) > 10000 -> "通话账户编号过长"
    else -> null
}

data class RestoreResult(val inserted: Int, val skipped: Int)

/** Preserve system changes to fields the user did not edit, even while the editor stayed open. */
internal fun CallEntry.mergeUneditedFields(baseline: CallEntry, current: CallEntry): CallEntry {
    require(id == baseline.id && id == current.id) { "编辑记录不匹配，请重新打开记录" }
    return copy(
        number = if (number == baseline.number) current.number else number,
        name = if (name == baseline.name) current.name else name,
        date = if (date == baseline.date) current.date else date,
        duration = if (duration == baseline.duration) current.duration else duration,
        type = if (type == baseline.type) current.type else type,
        accountId = if (accountId == baseline.accountId) current.accountId else accountId,
    )
}

class RestoreException(val result: RestoreResult, cause: Exception) : Exception(
    "恢复中断：已新增 ${result.inserted} 条，跳过 ${result.skipped} 条。${cause.localizedMessage ?: "系统未能写入记录"}", cause,
)

private data class DuplicateKey(val number: String, val name: String, val date: Long, val duration: Long, val type: Int, val accountId: String?)
private fun CallEntry.duplicateKey() = DuplicateKey(number, name, date, duration, type, accountId)

internal fun restoreEntries(existing: List<CallEntry>, incoming: List<CallEntry>, insert: (CallEntry) -> Unit): RestoreResult {
    require(incoming.size <= BackupCodec.MAX_ENTRIES) { "备份最多支持 50000 条记录" }
    incoming.forEachIndexed { index, entry ->
        require(entry.validate() == null) { "第 ${index + 1} 条记录：${entry.validate()}" }
    }
    val seen = existing.mapTo(HashSet()) { it.duplicateKey() }
    var inserted = 0
    var skipped = 0
    for (entry in incoming) {
        val key = entry.duplicateKey()
        if (key in seen) {
            skipped++
            continue
        }
        try {
            insert(entry.copy(id = 0))
            inserted++
            seen.add(key)
        } catch (error: Exception) {
            throw RestoreException(RestoreResult(inserted, skipped), error)
        }
    }
    return RestoreResult(inserted, skipped)
}

internal object BackupCodec {
    const val MAX_ENTRIES = 50000
    private const val MAX_BYTES = 20 * 1024 * 1024
    private const val FORMAT = "com.android.calleditor.calllog"

    fun encode(entries: List<CallEntry>): String {
        require(entries.size <= MAX_ENTRIES) { "备份最多支持 50000 条记录" }
        val records = JSONArray()
        entries.forEachIndexed { index, entry ->
            require(entry.validate() == null) { "第 ${index + 1} 条记录无法备份：${entry.validate()}" }
            records.put(JSONObject().apply {
                put("number", entry.number)
                put("name", entry.name)
                put("date", entry.date)
                put("duration", entry.duration)
                put("type", entry.type)
                put("accountId", entry.accountId ?: JSONObject.NULL)
            })
        }
        return JSONObject().put("format", FORMAT).put("version", 1).put("entries", records).toString()
            .also(::validateSize)
    }

    fun decode(text: String): List<CallEntry> {
        validateSize(text)
        try {
            val tokener = JSONTokener(text.removePrefix("\uFEFF"))
            val root = tokener.nextValue() as? JSONObject ?: throw IllegalArgumentException("备份根节点必须是对象")
            require(tokener.nextClean() == '\u0000') { "备份末尾包含多余内容" }
            require(root.opt("format") == FORMAT) { "不支持此备份格式；旧版备份暂不支持导入" }
            require(root.integer("version") == 1L) { "不支持此备份版本" }
            val records = root.opt("entries") as? JSONArray ?: throw IllegalArgumentException("备份缺少记录列表")
            require(records.length() <= MAX_ENTRIES) { "备份最多支持 50000 条记录" }
            return List(records.length()) { index ->
                try {
                    val record = records.get(index) as? JSONObject ?: throw IllegalArgumentException("记录必须是对象")
                    val type = record.integer("type")
                    require(type in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() && isSupportedCallType(type.toInt())) { "通话类型无效（原始值：$type）" }
                    CallEntry(
                        number = record.string("number"),
                        name = record.string("name"),
                        date = record.integer("date"),
                        duration = record.integer("duration"),
                        type = type.toInt(),
                        accountId = when (val value = record.opt("accountId")) {
                            null, JSONObject.NULL -> null
                            is String -> value
                            else -> throw IllegalArgumentException("accountId 必须是文本或 null")
                        },
                    ).also { require(it.validate() == null) { it.validate().orEmpty() } }
                } catch (error: Exception) {
                    throw IllegalArgumentException("第 ${index + 1} 条记录无效：${error.localizedMessage}", error)
                }
            }
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Exception) {
            throw IllegalArgumentException("备份 JSON 格式无效：${error.localizedMessage}", error)
        }
    }

    private fun JSONObject.string(key: String): String = opt(key) as? String
        ?: throw IllegalArgumentException("$key 必须是文本")

    private fun validateSize(text: String) {
        require(text.length <= MAX_BYTES && text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "备份文件不能超过 20 MB"
        }
    }

    private fun JSONObject.integer(key: String): Long = when (val value = opt(key)) {
        is Int -> value.toLong()
        is Long -> value
        else -> throw IllegalArgumentException("$key 必须是有效的整数")
    }
}
