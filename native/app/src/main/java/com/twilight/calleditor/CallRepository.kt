package com.twilight.calleditor

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.CallLog.Calls

/** All methods are blocking: invoke from Dispatchers.IO, with call-log permissions granted. */
class CallRepository(context: Context) {
    val vendorSupported = supportsVendorCallStatus(Build.MODEL, Build.VERSION.SDK_INT)
    private val resolver = context.applicationContext.contentResolver
    private val projection = arrayOf(Calls._ID, Calls.NUMBER, Calls.CACHED_NAME, Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.PHONE_ACCOUNT_ID)

    fun load(): List<CallEntry> = query()

    private fun query(id: Long? = null): List<CallEntry> {
        val uri = if (id == null) Calls.CONTENT_URI else ContentUris.withAppendedId(Calls.CONTENT_URI, id)
        // A null projection reads the columns the Provider actually exposes, avoiding a query
        // failure on systems that do not expose the optional virtual_call_id column.
        val cursor = resolver.query(uri, if (vendorSupported) null else projection, null, null, "${Calls.DATE} DESC, ${Calls._ID} DESC")
            ?: throw IllegalStateException("系统未返回通话记录，请检查权限后重试")
        return cursor.use {
            buildList {
                while (it.moveToNext()) add(it.toEntry())
            }
        }
    }

    private fun Cursor.toEntry() = CallEntry(
        id = getLong(getColumnIndexOrThrow(Calls._ID)), number = getString(getColumnIndexOrThrow(Calls.NUMBER)).orEmpty(), name = getString(getColumnIndexOrThrow(Calls.CACHED_NAME)).orEmpty(),
        date = getLong(getColumnIndexOrThrow(Calls.DATE)), duration = getLong(getColumnIndexOrThrow(Calls.DURATION)), type = getInt(getColumnIndexOrThrow(Calls.TYPE)), accountId = getString(getColumnIndexOrThrow(Calls.PHONE_ACCOUNT_ID)),
        vendorDetails = if (vendorSupported) VendorCallDetails(
            getColumnIndex(Calls.FEATURES).takeIf { it >= 0 && !isNull(it) }?.let { getInt(it) },
            getColumnIndex("virtual_call_id").takeIf { it >= 0 && !isNull(it) }?.let { getString(it) },
        ) else null,
    )

    fun save(draft: CallEntry, breenoEnabled: Boolean? = null, baseline: CallEntry? = null) {
        require(draft.validate() == null) { draft.validate().orEmpty() }
        val original = if (draft.id == 0L) null else {
            query(draft.id).singleOrNull()
                ?: throw IllegalStateException("此通话记录已不存在，请刷新列表")
        }
        val entry = if (original != null && baseline != null) draft.mergeUneditedFields(baseline, original) else draft
        val breenoEdit = planBreenoEdit(entry, original, breenoEnabled, vendorSupported)
        val values = ContentValues().apply {
            if (original == null || entry.number != original.number) put(Calls.NUMBER, entry.number)
            if (original == null || entry.name != original.name) put(Calls.CACHED_NAME, entry.name)
            if (original == null || entry.date != original.date) put(Calls.DATE, entry.date)
            if (original == null || entry.duration != original.duration) put(Calls.DURATION, entry.duration)
            if (original == null || entry.type != original.type) put(Calls.TYPE, entry.type)
            if (entry.accountId != null && (original == null || entry.accountId != original.accountId)) {
                put(Calls.PHONE_ACCOUNT_ID, entry.accountId)
            }
            if (breenoEdit != null) put(Calls.FEATURES, breenoEdit.features)
        }
        if (entry.id == 0L) {
            check(resolver.insert(Calls.CONTENT_URI, values) != null) { "系统未能新增通话记录" }
        } else if (values.size() > 0) {
            // Compare-and-set prevents a simultaneous system metadata update from losing bits.
            val selection = if (breenoEdit != null) "${Calls.FEATURES} = ? AND virtual_call_id = ?" else null
            val args = breenoEdit?.let { arrayOf(it.expectedFeatures.toString(), it.virtualCallId) }
            check(resolver.update(ContentUris.withAppendedId(Calls.CONTENT_URI, entry.id), values, selection, args) == 1) {
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
    fun restore(entries: List<CallEntry>): RestoreResult = restoreEntries(load(), entries) { save(it) }
}
