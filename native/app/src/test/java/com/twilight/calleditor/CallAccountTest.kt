package com.twilight.calleditor

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CallAccountTest {
    private fun record(component: String): CallEntry {
        val root = JSONObject(BackupCodec.encode(listOf(CallEntry(number = "000271001", date = 1700000000000, duration = 60, type = 2, accountId = "same-id"))))
        root.getJSONArray("entries").getJSONObject(0).put("accountComponent", component)
        return BackupCodec.decode(root.toString()).single()
    }

    @Test fun backupPreservesPhoneAccountComponent() {
        val json = JSONObject(BackupCodec.encode(listOf(record("example.one/.Calls"))))
        assertEquals("example.one/.Calls", json.getJSONArray("entries").getJSONObject(0).optString("accountComponent"))
    }

    @Test fun identicalAccountIdsFromDifferentServicesAreNotDuplicates() {
        val first = record("example.one/.Calls")
        val second = record("example.two/.Calls")
        val inserted = mutableListOf<CallEntry>()
        assertEquals(RestoreResult(1, 1), restoreEntries(listOf(first), listOf(second, second), inserted::add))
        assertEquals(listOf(second), inserted)
    }

    @Test fun oldBackupStillReadsAndSkipsExistingCompleteAccount() {
        val current = record("example.one/.Calls")
        val root = JSONObject(BackupCodec.encode(listOf(current))).put("version", 1)
        root.getJSONArray("entries").getJSONObject(0).remove("accountComponent")
        val old = BackupCodec.decode(root.toString()).single()
        assertNull(old.accountComponent)
        assertEquals(RestoreResult(0, 1), restoreEntries(listOf(current), listOf(old)) { fail("Legacy duplicate inserted") })
    }

    @Test fun uneditedAccountPreservesConcurrentChangesAsAPair() {
        val baseline = record("example.one/.Calls").copy(id = 5)
        val current = baseline.copy(accountId = "new-id", accountComponent = "example.two/.Calls")
        val merged = baseline.copy(duration = 123).mergeUneditedFields(baseline, current)
        assertEquals(123L, merged.duration)
        assertEquals(current.accountId, merged.accountId)
        assertEquals(current.accountComponent, merged.accountComponent)
    }

    @Test fun explicitAccountChangeKeepsSelectedPairEvenWhenIdIsUnchanged() {
        val baseline = record("example.one/.Calls").copy(id = 5)
        val draft = baseline.copy(accountComponent = "example.two/.Calls")
        val current = baseline.copy(accountId = "concurrent-id", duration = 99)
        val merged = draft.mergeUneditedFields(baseline, current)
        assertEquals(draft.accountId, merged.accountId)
        assertEquals(draft.accountComponent, merged.accountComponent)
        assertEquals(99L, merged.duration)
    }

    @Test fun invalidComponentIsRejectedBeforeRestore() {
        val root = JSONObject(BackupCodec.encode(listOf(record("example.one/.Calls"))))
        assertEquals(2, root.getInt("version"))
        val item = root.getJSONArray("entries").getJSONObject(0)
        item.put("accountComponent", 123)
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(root.toString()) }
        item.remove("accountComponent")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(root.toString()) }
        item.put("accountComponent", "x".repeat(10001))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(root.toString()) }
    }
}
