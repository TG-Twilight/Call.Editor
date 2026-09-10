package com.twilight.calleditor

import org.junit.Assert.*
import org.junit.Test

class CallStatusTest {
    private val call = CallEntry(number = "0000000910", date = 1_700_000_000_000, duration = 58, type = -1)

    @Test fun knownTypeNamesAndChoicesDoNotDependOnTheBreenoDeviceProfile() {
        assertTrue(supportsVendorCallStatus("RMX5200", 36))
        assertFalse(supportsVendorCallStatus("RMX3031", 35))
        assertFalse(supportsVendorCallStatus("RMX5200", 37))
        assertFalse(supportsVendorCallStatus("other", 36))
        assertEquals("呼入（黑名单归类）", call.statusLabel(false))
        assertEquals("呼入（黑名单归类）", call.statusLabel(true))
        assertTrue(-1 in editableCallTypes(1))
        assertTrue(-99 in editableCallTypes(-99))
        assertFalse(-27 in editableCallTypes(1))
        val names = mapOf(-1 to "呼入（黑名单归类）", -3 to "未接（黑名单归类）", -5 to "已拒接（黑名单归类）",
            27 to "黑名单号码（拦截分类）", 51 to "骚扰电话（拦截分类）", 52 to "广告推销（拦截分类）")
        names.forEach { (type, label) ->
            assertEquals(label, call.copy(type = type).statusLabel(false))
            assertEquals(label, typeLabel(type))
            assertTrue(type in editableCallTypes(1))
        }
    }

    @Test fun breenoRequiresBothIndependentFieldsAndNeverJustANegativeType() {
        assertFalse(call.isBreenoCall(true))
        assertFalse(call.copy(vendorDetails = VendorCallDetails(0x20000040, null)).isBreenoCall(true))
        assertFalse(call.copy(vendorDetails = VendorCallDetails(0x20000040, "")).isBreenoCall(true))
        assertFalse(call.copy(vendorDetails = VendorCallDetails(0x10000040, "fixture-id")).isBreenoCall(true))
        val answered = call.copy(vendorDetails = VendorCallDetails(0x20000040, "fixture-id"))
        assertTrue(answered.isBreenoCall(true))
        assertFalse(answered.isBreenoCall(false))
        assertEquals("呼入（黑名单归类） · 小布代接", answered.statusLabel(true))
        assertTrue(answered.copy(type = 1).isBreenoCall(true))
    }

    @Test fun filtersKeepAnsweringBlacklistAndInterceptionSeparate() {
        assertTrue(call.matchesCallFilter(1, true))
        assertTrue(call.matchesCallFilter(FILTER_BLACKLIST, true))
        assertFalse(call.matchesCallFilter(6, true))
        assertFalse(call.matchesCallFilter(FILTER_BREENO, true))
        assertTrue(call.copy(type = -3).matchesCallFilter(3, true))
        assertTrue(call.copy(type = -5).matchesCallFilter(5, true))
        assertTrue(call.copy(type = 51, duration = 60).matchesCallFilter(6, true))
        assertFalse(call.copy(type = 51).matchesCallFilter(FILTER_BLACKLIST, true))
        assertTrue(call.copy(type = 27).matchesCallFilter(FILTER_BLACKLIST, true))
        assertTrue(call.matchesCallFilter(1, false))
        assertFalse(call.matchesCallFilter(FILTER_OTHER, false))
        assertTrue(call.copy(type = -3).matchesCallFilter(3, false))
        assertTrue(call.copy(type = 51).matchesCallFilter(6, false))
        assertTrue(call.matchesCallFilter(FILTER_BLACKLIST, false))
        assertFalse(call.matchesCallFilter(FILTER_OTHER, true))
        assertTrue(call.copy(type = -27).matchesCallFilter(FILTER_OTHER, true))
    }

    @Test fun togglingBreenoPreservesAllOtherBitsAndRequiresAnExistingLink() {
        assertEquals(0x40, editedBreenoFeatures(VendorCallDetails(0x20000040, "fixture-id"), false))
        assertEquals(0x30000041, editedBreenoFeatures(VendorCallDetails(0x10000041, "fixture-id"), true))
        assertEquals(0xB0000041.toInt(), editedBreenoFeatures(VendorCallDetails(0x90000041.toInt(), "fixture-id"), true))
        assertThrows(IllegalArgumentException::class.java) { editedBreenoFeatures(null, true) }
        assertThrows(IllegalArgumentException::class.java) { editedBreenoFeatures(VendorCallDetails(null, "fixture-id"), true) }
        assertThrows(IllegalArgumentException::class.java) { editedBreenoFeatures(VendorCallDetails(64, null), true) }
        assertThrows(IllegalArgumentException::class.java) { editedBreenoFeatures(VendorCallDetails(64, ""), true) }
    }

    @Test fun basicBackupStillDeduplicatesRecordsWithLiveVendorDetails() {
        val live = call.copy(vendorDetails = VendorCallDetails(0x20000040, "fixture-id"))
        val parsed = BackupCodec.decode(BackupCodec.encode(listOf(live)))
        assertEquals(listOf(call), parsed)
        assertEquals(RestoreResult(0, 1), restoreEntries(listOf(live), parsed) { fail("Duplicate inserted") })
        assertEquals(live.vendorDetails, live.copy(type = 51, duration = 61).vendorDetails)
    }

    @Test fun unchangedBreenoDoesNotOverwriteMetadataUpdatedByTheSystem() {
        val opened = call.copy(id = 123, vendorDetails = VendorCallDetails(0x20000040, "fixture-id"))
        val refreshed = opened.copy(vendorDetails = VendorCallDetails(0x10000040, "different-fixture-id"))
        assertNull(planBreenoEdit(opened.copy(duration = 80), refreshed, null, true))
        assertThrows(IllegalArgumentException::class.java) { planBreenoEdit(opened, refreshed, false, true) }
        assertThrows(IllegalArgumentException::class.java) { planBreenoEdit(opened, opened, false, false) }
        assertThrows(IllegalArgumentException::class.java) { planBreenoEdit(opened.copy(id = 0), null, true, true) }
        val edit = planBreenoEdit(opened, opened, false, true)!!
        assertEquals(64, edit.features)
        assertEquals(0x20000040, edit.expectedFeatures)
        assertEquals("fixture-id", edit.virtualCallId)
    }

    @Test fun onlyChangingBreenoKeepsBasicFieldsUpdatedByTheSystem() {
        val opened = call.copy(id = 123, type = 1, vendorDetails = VendorCallDetails(0x20000040, "fixture-id"))
        val current = opened.copy(type = -1, name = "Updated fixture", duration = 80, accountId = "sim2", date = opened.date + 1000, number = "0000000911")
        val saved = opened.mergeUneditedFields(opened, current)
        assertEquals(current, saved)
        assertEquals(64, planBreenoEdit(saved, current, false, true)!!.features)
        val edited = opened.copy(duration = 99, type = 51).mergeUneditedFields(opened, current)
        assertEquals(current.copy(duration = 99, type = 51), edited)
    }
}
