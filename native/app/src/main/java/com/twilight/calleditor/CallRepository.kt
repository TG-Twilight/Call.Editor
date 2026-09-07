package com.twilight.calleditor

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CallLog.Calls

/** All methods are blocking: invoke from Dispatchers.IO, with call-log permissions granted. */
class CallRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val projection = arrayOf(Calls._ID, Calls.NUMBER, Calls.CACHED_NAME, Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.PHONE_ACCOUNT_ID)

    fun load(): List<CallEntry> = query()

    private fun query(id: Long? = null): List<CallEntry> {
        val uri = if (id == null) Calls.CONTENT_URI else ContentUris.withAppendedId(Calls.CONTENT_URI, id)
        val cursor = resolver.query(uri, projection, null, null, "${Calls.DATE} DESC, ${Calls._ID} DESC")
            ?: throw IllegalStateException("系统未返回通话记录，请检查权限后重试")
        return cursor.use {
            buildList {
                while (it.moveToNext()) add(it.toEntry())
            }
        }
    }

    private fun Cursor.toEntry() = CallEntry(
        id = getLong(0), number = getString(1).orEmpty(), name = getString(2).orEmpty(),
        date = getLong(3), duration = getLong(4), type = getInt(5), accountId = getString(6),
    )

    fun save(entry: CallEntry) {
        require(entry.validate() == null) { entry.validate().orEmpty() }
        val original = if (entry.id == 0L) null else {
            query(entry.id).singleOrNull()
                ?: throw IllegalStateException("此通话记录已不存在，请刷新列表")
        }
        val values = ContentValues().apply {
            if (original == null || entry.number != original.number) put(Calls.NUMBER, entry.number)
            if (original == null || entry.name != original.name) put(Calls.CACHED_NAME, entry.name)
            if (original == null || entry.date != original.date) put(Calls.DATE, entry.date)
            if (original == null || entry.duration != original.duration) put(Calls.DURATION, entry.duration)
            if (original == null || entry.type != original.type) put(Calls.TYPE, entry.type)
            if (entry.accountId != null && (original == null || entry.accountId != original.accountId)) {
                put(Calls.PHONE_ACCOUNT_ID, entry.accountId)
            }
        }
        if (entry.id == 0L) {
            check(resolver.insert(Calls.CONTENT_URI, values) != null) { "系统未能新增通话记录" }
        } else if (values.size() > 0) {
            check(resolver.update(ContentUris.withAppendedId(Calls.CONTENT_URI, entry.id), values, null, null) == 1) {
                "系统未能更新此通话记录，请刷新后重试"
            }
        }
    }

    fun delete(id: Long) {
        require(id > 0) { "记录编号无效" }
        check(resolver.delete(Calls.CONTENT_URI, "${Calls._ID} = ?", arrayOf(id.toString())) == 1) {
            "系统未能删除此通话记录，记录可能已不存在"
        }
    }

    fun exportJson(): String = BackupCodec.encode(load())
    fun parseBackup(text: String): List<CallEntry> = BackupCodec.decode(text)
    fun restore(entries: List<CallEntry>): RestoreResult = restoreEntries(load(), entries, ::save)
}
