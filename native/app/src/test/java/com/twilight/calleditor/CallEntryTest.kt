package com.twilight.calleditor

import org.junit.Assert.*
import org.junit.Test

class CallEntryTest {
    private val entry = CallEntry(number = "+886123456", name = "測試", date = 1_700_000_000_000, duration = 60, type = 1, accountId = "sim1")

    @Test fun observedTypesAreNamedWithoutConfusingBlacklistAndAnswering() {
        assertEquals("呼入（黑名单归类）", typeLabel(-1))
        assertEquals("未接（黑名单归类）", typeLabel(-3))
        assertEquals("已拒接（黑名单归类）", typeLabel(-5))
        assertEquals("黑名单号码（拦截分类）", typeLabel(27))
        assertEquals("骚扰电话（拦截分类）", typeLabel(51))
        assertEquals("广告推销（拦截分类）", typeLabel(52))
        assertEquals("其他（-27）", typeLabel(-27))
    }

    @Test fun validationRejectsInvalidDatesDurationsAndTypes() {
        assertNull(entry.validate())
        assertNotNull(entry.copy(date = 0).validate())
        assertNotNull(entry.copy(date = Long.MAX_VALUE).validate())
        assertNotNull(entry.copy(duration = -1).validate())
        assertNotNull(entry.copy(type = 0).validate())
        assertNotNull(entry.copy(type = 8).validate())
        assertNotNull(entry.copy(type = 99).validate())
        assertNull(entry.copy(number = "", type = 7).validate())
    }

    @Test fun observedVendorTypesSurviveBackupAndRestoreWithoutConversion() {
        val vendorEntries = listOf(-5, -3, -1, 27, 51, 52).map { entry.copy(type = it) }
        vendorEntries.forEach { assertNull(it.validate()) }
        val parsed = BackupCodec.decode(BackupCodec.encode(vendorEntries))
        assertEquals(vendorEntries, parsed)
        val inserted = mutableListOf<CallEntry>()
        assertEquals(RestoreResult(vendorEntries.size, 0), restoreEntries(emptyList(), parsed, inserted::add))
        assertEquals(vendorEntries, inserted)
        assertEquals(RestoreResult(0, vendorEntries.size), restoreEntries(inserted, parsed) { fail("Duplicate vendor record inserted") })
    }

    @Test fun rmx5200ExportPreservesNegativeTypeAtRecord29() {
        val records = List(28) { entry.copy(date = entry.date - it) } +
            entry.copy(date = entry.date - 28, type = -3, duration = 0)
        val parsed = BackupCodec.decode(BackupCodec.encode(records))
        assertEquals(records, parsed)
        assertEquals(-3, parsed[28].type)
    }

    @Test fun unsupportedTypeErrorIdentifiesRecordAndRawValue() {
        val invalid = List(28) { entry } + entry.copy(type = -99)
        val error = assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(invalid) }
        assertEquals("第 29 条记录无法备份：通话类型无效（原始值：-99）", error.message)
    }

    @Test fun backupRejectsUnknownAndOverflowingTypes() {
        val valid = BackupCodec.encode(listOf(entry))
        for (type in listOf("0", "8", "99", "4294967297")) {
            val invalid = valid.replace("\"type\":1", "\"type\":$type")
            assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(invalid) }
        }
    }

    @Test fun backupRoundTripPreservesFieldsButDoesNotRestoreProviderIds() {
        val parsed = BackupCodec.decode(BackupCodec.encode(listOf(entry.copy(id = 123), entry.copy(number = "", accountId = null))))
        assertEquals(listOf(entry, entry.copy(number = "", accountId = null)), parsed)
    }

    @Test fun malformedBackupIsRejectedBeforeAnyInsertion() {
        val valid = BackupCodec.encode(listOf(entry))
        for (bad in listOf(
            valid.replace("\"version\":1", "\"version\":2"),
            valid.replace("\"duration\":60", "\"duration\":-1"),
            valid.replace("\"duration\":60", "\"duration\":1.5"),
            valid.replace("\"duration\":60", "\"duration\":\"60\""),
            valid.replace("\"date\":1700000000000", "\"date\":9223372036854775808"),
            valid.replace("\"number\":\"+886123456\"", "\"number\":null"),
            valid + " garbage"
        )) {
            assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(bad) }
        }
        val inserted = mutableListOf<CallEntry>()
        assertThrows(IllegalArgumentException::class.java) {
            restoreEntries(emptyList(), listOf(entry, entry.copy(type = 99)), inserted::add)
        }
        assertTrue(inserted.isEmpty())
    }

    @Test fun restoreSkipsExactDuplicatesIgnoringOnlyProviderIdAndPreservesDifferentNames() {
        val inserted = mutableListOf<CallEntry>()
        val otherSim = entry.copy(accountId = "sim2")
        val otherName = entry.copy(name = "new name")
        val result = restoreEntries(listOf(entry), listOf(entry.copy(id = 99), otherName, otherName.copy(id = 100), otherSim, otherSim), inserted::add)
        assertEquals(RestoreResult(2, 3), result)
        assertEquals(listOf(otherName, otherSim), inserted)
    }

    @Test fun backupSizeLimitUsesUtf8BytesForBothExportAndImport() {
        val multilingualEntry = entry.copy(name = "測".repeat(10000))
        val oversizedEntries = List(700) { multilingualEntry }
        val encodeError = assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(oversizedEntries) }
        assertEquals("备份文件不能超过 20 MB", encodeError.message)

        // Build independently so an export rejection cannot hide a missing import limit.
        val record = org.json.JSONObject(BackupCodec.encode(listOf(multilingualEntry)))
            .getJSONArray("entries").getJSONObject(0).toString()
        val oversizedJson = "{\"format\":\"com.android.calleditor.calllog\",\"version\":1,\"entries\":[" +
            List(700) { record }.joinToString(",") + "]}"
        assertTrue(oversizedJson.length < 20 * 1024 * 1024)
        assertTrue(oversizedJson.toByteArray(Charsets.UTF_8).size > 20 * 1024 * 1024)
        val decodeError = assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(oversizedJson) }
        assertEquals("备份文件不能超过 20 MB", decodeError.message)
        val underLimitEntries = List(690) { multilingualEntry }
        assertEquals(underLimitEntries, BackupCodec.decode(BackupCodec.encode(underLimitEntries)))
    }

    @Test fun restoreReportsPartialProgress() {
        var calls = 0
        val failure = assertThrows(RestoreException::class.java) {
            restoreEntries(emptyList(), listOf(entry, entry.copy(date = entry.date + 1))) {
                calls++
                if (calls == 2) error("provider failed")
            }
        }
        assertEquals(RestoreResult(1, 0), failure.result)
        assertEquals("provider failed", failure.cause?.message)
    }
}
